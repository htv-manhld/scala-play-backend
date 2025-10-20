package application.user

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import domain.user.{User, UserId, Email, UserProfile, UserRepository, UserDomainService, PasswordHasher}
import domain.shared.{DomainError, PaginatedResponse}
import infrastructure.messaging.EventPublisher
import application.shared.{ApplicationBase, LoggingService}
import application.user.commands._
import application.user.queries._

/**
 * Unified User Service (Application Layer)
 * Handles both command (write) and query (read) operations
 * Orchestrates domain services, repositories, and infrastructure services
 */
@Singleton
class UserService @Inject()(
  userRepository: UserRepository,
  userDomainService: UserDomainService,
  passwordHasher: PasswordHasher,
  eventPublisher: EventPublisher,
  loggingService: LoggingService
)(implicit ec: ExecutionContext) extends ApplicationBase {

  // Query operations (Read)
  def handle(query: GetUserByIdQuery): Future[Option[User]] = {
    loggingService.timed(s"GetUserByIdQuery", query.userId.value) {
      userRepository.findById(query.userId)
    }
  }

  def handle(query: GetUserByEmailQuery): Future[Option[User]] = {
    userRepository.findByEmail(query.email)
  }

  def handle(query: GetAllUsersQuery): Future[Seq[User]] = {
    userRepository.findAll(
      query.limit,
      query.search,
      query.orderBy,
      query.orderDirection,
      query.ignoreId,
      query.status,
      query.createdFrom,
      query.createdTo
    )
  }

  def handle(query: GetUsersPaginatedQuery): Future[PaginatedResponse[User]] = {
    userRepository.findAllPaginated(
      query.page,
      query.size,
      query.search,
      query.orderBy,
      query.orderDirection,
      query.ignoreId,
      query.status,
      query.createdFrom,
      query.createdTo
    )
  }

  // Command operations (Write)
  def handle(command: CreateUserCommand): Future[Either[DomainError, User]] = {
    executeWithLogging("CreateUserCommand", Map("email" -> command.email.value, "name" -> command.name)) {
      // Hash password if provided (Infrastructure concern)
      val hashedPassword = command.password.map(passwordHasher.hash)

      // Create user profile
      val userProfile = UserProfile(command.name, command.birthdate)

      for {
        // Use Domain Service for user creation (includes all business validations)
        userCreationResult <- userDomainService.createUser(command.email, userProfile, hashedPassword)

        result <- userCreationResult match {
          case Right(user) =>
            // Save to repository
            for {
              savedUserResult <- userRepository.save(user)
              result <- savedUserResult match {
                case Right(savedUser) =>
                  // Publish domain events
                  eventPublisher.publishAll(savedUser.uncommittedEvents).map { _ =>
                    val committedUser = savedUser.markEventsAsCommitted()
                    Right(committedUser)
                  }
                case Left(error) => Future.successful(Left(error))
              }
            } yield result
          case Left(error) =>
            Future.successful(Left(error))
        }
      } yield result
    }
  }

  def handle(command: UpdateUserProfileCommand): Future[Either[DomainError, User]] = {
    for {
      userOption <- userRepository.findById(command.userId)
      result <- userOption match {
        case Some(user) =>
          try {
            val newProfile = UserProfile(command.name, command.birthdate)
            val updatedUser = user.changeProfile(newProfile)

            for {
              saveResult <- userRepository.save(updatedUser)
              result <- saveResult match {
                case Right(savedUser) =>
                  eventPublisher.publishAll(savedUser.uncommittedEvents).map { _ =>
                    savedUser.markEventsAsCommitted()
                    Right(savedUser)
                  }
                case Left(error) => Future.successful(Left(error))
              }
            } yield result
          } catch {
            case ex: IllegalArgumentException =>
              Future.successful(Left(DomainError.ValidationError(ex.getMessage)))
          }
        case None =>
          Future.successful(Left(DomainError.NotFound(s"User with id ${command.userId.value} not found")))
      }
    } yield result
  }

  def handle(command: ChangeUserEmailCommand): Future[Either[DomainError, User]] = {
    for {
      // Get the user
      userOption <- userRepository.findById(command.userId)
      result <- userOption match {
        case Some(user) =>
          for {
            // Use Domain Service to validate email change
            validationResult <- userDomainService.validateEmailChange(user, command.newEmail)
            result <- validationResult match {
              case Right(_) =>
                // Domain validation passed, now change email using aggregate method
                user.changeEmail(command.newEmail) match {
                  case Right(updatedUser) =>
                    for {
                      saveResult <- userRepository.save(updatedUser)
                      result <- saveResult match {
                        case Right(savedUser) =>
                          eventPublisher.publishAll(savedUser.uncommittedEvents).map { _ =>
                            val committedUser = savedUser.markEventsAsCommitted()
                            Right(committedUser)
                          }
                        case Left(error) => Future.successful(Left(error))
                      }
                    } yield result
                  case Left(error) => Future.successful(Left(error))
                }
              case Left(error) =>
                Future.successful(Left(error))
            }
          } yield result
        case None =>
          Future.successful(Left(DomainError.NotFound(s"User with id ${command.userId.value} not found")))
      }
    } yield result
  }

  def handle(command: DeleteUserCommand): Future[Either[DomainError, Unit]] = {
    for {
      // Get the user
      userOption <- userRepository.findById(command.userId)
      result <- userOption match {
        case Some(user) =>
          // Use Domain Service to check if user can be deleted (uses Specification)
          userDomainService.canDeleteUser(user) match {
            case Right(_) =>
              // Business rule satisfied, proceed with deletion
              userRepository.delete(command.userId)
            case Left(error) =>
              Future.successful(Left(error))
          }
        case None =>
          Future.successful(Left(DomainError.NotFound(s"User with id ${command.userId.value} not found")))
      }
    } yield result
  }
}