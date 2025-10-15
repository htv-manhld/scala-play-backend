package application.user

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import domain.user._
import domain.user.events.{UserLoggedIn, LoginFailed}
import domain.shared.DomainError
import infrastructure.messaging.EventPublisher
import application.shared.{ApplicationBase, LoggingService}
import java.time.LocalDateTime

/**
 * User Authentication Service (Application Layer)
 * Handles authentication-related operations
 */
@Singleton
class UserAuthService @Inject()(
  userRepository: UserRepository,
  passwordHasher: PasswordHasher,
  tokenGenerator: TokenGenerator,
  eventPublisher: EventPublisher,
  loggingService: LoggingService,
  tokenBlacklist: infrastructure.security.TokenBlacklist,
  passwordResetTokenRepository: domain.user.PasswordResetTokenRepository,
  emailService: infrastructure.email.EmailService
)(implicit ec: ExecutionContext) extends ApplicationBase {

  /**
   * Authenticate user and generate JWT token
   */
  def login(email: Email, password: String): Future[Either[DomainError, (User, AuthToken)]] = {
    executeWithLogging("LoginUser", Map("email" -> email.value)) {
      for {
        userOption <- userRepository.findByEmail(email)
        result <- userOption match {
          case Some(user) =>
            // Verify password
            if (user.password.isEmpty) {
              // User has no password set (e.g., OAuth user)
              val event = LoginFailed(email, "No password set for this account", LocalDateTime.now())
              eventPublisher.publish(event).map { _ =>
                Left(DomainError.AuthenticationFailed("No password set for this account"))
              }
            } else if (!passwordHasher.verify(password, user.password.get)) {
              // Invalid password
              val event = LoginFailed(email, "Invalid password", LocalDateTime.now())
              eventPublisher.publish(event).map { _ =>
                Left(DomainError.AuthenticationFailed("Invalid email or password"))
              }
            } else if (!user.isActive) {
              // Account is not active
              val event = LoginFailed(email, "Account is not active", LocalDateTime.now())
              eventPublisher.publish(event).map { _ =>
                Left(DomainError.AuthenticationFailed("Account is not active"))
              }
            } else {
              // Authentication successful
              val userId = user.id.value.getOrElse(
                throw new IllegalStateException("User.id should not be None for persisted users")
              )

              // Generate token
              val token = tokenGenerator.generateToken(userId, email.value)

              // Update last login time
              val updatedUser = user.recordLogin()

              // Save updated user and publish event
              for {
                saveResult <- userRepository.save(updatedUser)
                result <- saveResult match {
                  case Right(savedUser) =>
                    // Publish UserLoggedIn event
                    val event = UserLoggedIn(savedUser.id, LocalDateTime.now())
                    eventPublisher.publish(event).map { _ =>
                      Right((savedUser, token))
                    }
                  case Left(error) =>
                    Future.successful(Left(error))
                }
              } yield result
            }

          case None =>
            // User not found
            val event = LoginFailed(email, "User not found", LocalDateTime.now())
            eventPublisher.publish(event).map { _ =>
              Left(DomainError.AuthenticationFailed("Invalid email or password"))
            }
        }
      } yield result
    }
  }

  /**
   * Verify JWT token and get user
   * Checks both token validity and blacklist
   */
  def verifyToken(token: String): Future[Either[DomainError, User]] = {
    tokenGenerator.verifyTokenWithBlacklist(token).flatMap {
      case Some((userId, _)) =>
        userRepository.findById(UserId.existing(userId)).map {
          case Some(user) =>
            if (user.isActive) {
              Right(user)
            } else {
              Left(DomainError.AuthenticationFailed("Account is not active"))
            }
          case None =>
            Left(DomainError.NotFound("User not found"))
        }
      case None =>
        Future.successful(Left(DomainError.AuthenticationFailed("Invalid or expired token")))
    }
  }

  /**
   * Refresh JWT token - Generate new token from existing valid token
   */
  def refreshToken(token: String): Future[Either[DomainError, (User, AuthToken)]] = {
    executeWithLogging("RefreshToken", Map("tokenPrefix" -> token.take(20))) {
      tokenGenerator.verifyToken(token) match {
        case Some((userId, email)) =>
          userRepository.findById(UserId.existing(userId)).map {
            case Some(user) =>
              if (user.isActive) {
                // Generate new token
                val newToken = tokenGenerator.generateToken(userId, email)
                Right((user, newToken))
              } else {
                Left(DomainError.AuthenticationFailed("Account is not active"))
              }
            case None =>
              Left(DomainError.NotFound("User not found"))
          }
        case None =>
          Future.successful(Left(DomainError.AuthenticationFailed("Invalid or expired token")))
      }
    }
  }

  /**
   * Logout - Blacklist the current token
   */
  def logout(token: String): Future[Either[DomainError, Unit]] = {
    executeWithLogging("LogoutUser", Map("tokenPrefix" -> token.take(20))) {
      // Get expiration time from token
      tokenGenerator.getExpirationTime(token) match {
        case Some(expiresAt) =>
          // Add token to blacklist
          tokenBlacklist.blacklistToken(token, expiresAt).map { _ =>
            Right(())
          }
        case None =>
          Future.successful(Left(DomainError.AuthenticationFailed("Invalid token")))
      }
    }
  }

  /**
   * Change password - Requires old password verification
   */
  def changePassword(userId: UserId, oldPassword: String, newPassword: String, currentToken: String): Future[Either[DomainError, Unit]] = {
    executeWithLogging("ChangePassword", Map("userId" -> userId.value.toString)) {
      for {
        userOption <- userRepository.findById(userId)
        result <- userOption match {
          case Some(user) =>
            // Verify old password
            if (user.password.isEmpty) {
              Future.successful(Left(DomainError.AuthenticationFailed("No password set for this account")))
            } else if (!passwordHasher.verify(oldPassword, user.password.get)) {
              Future.successful(Left(DomainError.AuthenticationFailed("Invalid old password")))
            } else {
              // Hash new password
              val hashedPassword = passwordHasher.hash(newPassword)
              val updatedUser = user.copy(password = Some(hashedPassword))

              // Save updated user
              for {
                saveResult <- userRepository.save(updatedUser)
                finalResult <- saveResult match {
                  case Right(_) =>
                    // Blacklist current token to force re-login
                    tokenGenerator.getExpirationTime(currentToken) match {
                      case Some(expiresAt) =>
                        tokenBlacklist.blacklistToken(currentToken, expiresAt).map { _ =>
                          Right(())
                        }
                      case None =>
                        Future.successful(Right(())) // Token invalid but password changed
                    }
                  case Left(error) =>
                    Future.successful(Left(error))
                }
              } yield finalResult
            }
          case None =>
            Future.successful(Left(DomainError.NotFound("User not found")))
        }
      } yield result
    }
  }

  /**
   * Forgot Password - Generate reset token and send email
   */
  def forgotPassword(email: Email, resetUrl: String): Future[Either[DomainError, Unit]] = {
    executeWithLogging("ForgotPassword", Map("email" -> email.value)) {
      for {
        userOption <- userRepository.findByEmail(email)
        result <- userOption match {
          case Some(user) if user.isActive =>
            // Invalidate any existing reset tokens for this user
            for {
              _ <- passwordResetTokenRepository.invalidateAllTokensForUser(user.id)

              // Create new reset token
              resetToken = domain.user.PasswordResetToken.create(user.id, expirationHours = 1)

              // Save reset token
              saveResult <- passwordResetTokenRepository.save(resetToken)

              // Send email
              finalResult <- saveResult match {
                case Right(savedToken) =>
                  emailService.sendPasswordResetEmail(
                    email.value,
                    savedToken.token,
                    resetUrl
                  ).map { emailSent =>
                    if (emailSent) {
                      Right(())
                    } else {
                      Left(DomainError.InvalidOperation("Failed to send reset email"))
                    }
                  }
                case Left(error) =>
                  Future.successful(Left(error))
              }
            } yield finalResult

          case Some(_) =>
            // User exists but not active
            Future.successful(Left(DomainError.AuthenticationFailed("Account is not active")))

          case None =>
            // User not found - for security, don't reveal if email exists
            // Return success but don't send email
            Future.successful(Right(()))
        }
      } yield result
    }
  }

  /**
   * Reset Password - Verify token and update password
   */
  def resetPassword(token: String, newPassword: String): Future[Either[DomainError, Unit]] = {
    executeWithLogging("ResetPassword", Map("tokenPrefix" -> token.take(20))) {
      for {
        resetTokenOption <- passwordResetTokenRepository.findByToken(token)
        result <- resetTokenOption match {
          case Some(resetToken) if resetToken.isValid =>
            // Token is valid, get user and update password
            for {
              userOption <- userRepository.findById(resetToken.userId)
              updateResult <- userOption match {
                case Some(user) =>
                  // Hash new password
                  val hashedPassword = passwordHasher.hash(newPassword)
                  val updatedUser = user.copy(password = Some(hashedPassword))

                  // Save user and mark token as used
                  for {
                    _ <- userRepository.save(updatedUser)
                    markedToken = resetToken.markAsUsed()
                    _ <- passwordResetTokenRepository.save(markedToken)
                  } yield Right(())

                case None =>
                  Future.successful(Left(DomainError.NotFound("User not found")))
              }
            } yield updateResult

          case Some(_) =>
            // Token exists but expired or already used
            Future.successful(Left(DomainError.AuthenticationFailed("Reset token is invalid or expired")))

          case None =>
            // Token not found
            Future.successful(Left(DomainError.AuthenticationFailed("Reset token is invalid or expired")))
        }
      } yield result
    }
  }
}
