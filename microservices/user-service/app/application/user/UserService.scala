package application.user

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import domain.user.{User, UserId, Email, UserProfile, UserRepository}
import domain.shared.DomainError
import infrastructure.messaging.EventPublisher
import application.shared.{ApplicationBase, LoggingService}
import application.user.commands._
import application.user.queries._

/**
 * Unified User Service
 * Handles both command (write) and query (read) operations
 */
@Singleton
class UserService @Inject()(
  userRepository: UserRepository,
  eventPublisher: EventPublisher,
  loggingService: LoggingService
)(implicit ec: ExecutionContext) extends ApplicationBase {

  // Query operations (Read)
  def handle(query: GetUserByIdQuery): Future[Option[User]] = {
    loggingService.timed(s"GetUserByIdQuery", Some(query.userId.value)) {
      userRepository.findById(query.userId)
    }
  }

  def handle(query: GetUserByEmailQuery): Future[Option[User]] = {
    userRepository.findByEmail(query.email)
  }

  def handle(query: GetAllUsersQuery): Future[Seq[User]] = {
    userRepository.findAll(query.page, query.size)
  }

  // Command operations (Write)
  def handle(command: CreateUserCommand): Future[Either[DomainError, User]] = {
    executeWithLogging("CreateUserCommand", Map("email" -> command.email.value, "name" -> command.name, "age" -> command.age.toString)) {
      for {
        // Check if user with email already exists
        existingUser <- userRepository.findByEmail(command.email)
        result <- existingUser match {
          case Some(_) =>
            Future.successful(Left(DomainError.DuplicateError(s"User with email ${command.email.value} already exists")))
          case None =>
            // Create domain user
            try {
              val userProfile = UserProfile(command.name, command.age)
              User.create(command.email, userProfile) match {
                case Right(user) =>
                  for {
                    savedUserResult <- userRepository.save(user)
                    result <- savedUserResult match {
                      case Right(savedUser) =>
                        // Publish domain events
                        eventPublisher.publishAll(savedUser.uncommittedEvents).map { _ =>
                          savedUser.markEventsAsCommitted()
                          Right(savedUser)
                        }
                      case Left(error) => Future.successful(Left(error))
                    }
                  } yield result
                case Left(error) => Future.successful(Left(error))
              }
            } catch {
              case ex: IllegalArgumentException =>
                Future.successful(Left(DomainError.ValidationError(ex.getMessage)))
            }
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
            val newProfile = UserProfile(command.name, command.age)
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
      // Check if new email is already taken
      existingUserWithEmail <- userRepository.findByEmail(command.newEmail)
      result <- existingUserWithEmail match {
        case Some(existingUser) if existingUser.id != command.userId =>
          Future.successful(Left(DomainError.DuplicateError(s"Email ${command.newEmail.value} is already taken")))
        case _ =>
          // Get the user to update
          for {
            userOption <- userRepository.findById(command.userId)
            result <- userOption match {
              case Some(user) =>
                user.changeEmail(command.newEmail) match {
                  case Right(updatedUser) =>
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
                  case Left(error) => Future.successful(Left(error))
                }
              case None =>
                Future.successful(Left(DomainError.NotFound(s"User with id ${command.userId.value} not found")))
            }
          } yield result
      }
    } yield result
  }

  def handle(command: DeleteUserCommand): Future[Either[DomainError, Unit]] = {
    userRepository.delete(command.userId)
  }
}