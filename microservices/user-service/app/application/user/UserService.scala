package application.user

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import domain.user._
import application.shared.{ApplicationService, Command, Query}
import java.time.LocalDateTime

/**
 * Application Service for User operations
 * Orchestrates domain operations and coordinates with infrastructure
 */
@Singleton
class UserApplicationService @Inject()(
  userRepository: UserRepository,
  userDomainService: UserDomainService,
  eventPublisher: EventPublisher
)(implicit ec: ExecutionContext) extends ApplicationService {

  def createUser(command: CreateUserCommand): Future[Either[DomainError, UserId]] = {
    for {
      isEmailUnique <- userDomainService.isEmailUnique(command.email)
      result <- if (isEmailUnique) {
        for {
          userId <- userRepository.nextId()
          user = User(
            id = userId,
            email = command.email,
            profile = UserProfile(command.name, command.age),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
          ).addEvent(UserCreated(userId, command.email, UserProfile(command.name, command.age), LocalDateTime.now()))
          savedUser <- userRepository.save(user)
          _ <- eventPublisher.publishAll(savedUser.getUncommittedEvents)
        } yield Right(savedUser.id)
      } else {
        Future.successful(Left(DomainError.ValidationError("Email already exists")))
      }
    } yield result
  }

  def updateUser(command: UpdateUserCommand): Future[Either[DomainError, Unit]] = {
    userRepository.findById(command.userId).flatMap {
      case Some(user) =>
        val updatedUser = user.changeProfile(UserProfile(command.name, command.age))
        for {
          _ <- userRepository.save(updatedUser)
          _ <- eventPublisher.publishAll(updatedUser.getUncommittedEvents)
        } yield Right(())
      case None =>
        Future.successful(Left(DomainError.NotFound(s"User not found: ${command.userId.value}")))
    }
  }

  def changeUserEmail(command: ChangeUserEmailCommand): Future[Either[DomainError, Unit]] = {
    userRepository.findById(command.userId).flatMap {
      case Some(user) =>
        user.changeEmail(command.newEmail) match {
          case Right(updatedUser) =>
            for {
              isEmailUnique <- userDomainService.isEmailUnique(command.newEmail, Some(command.userId))
              result <- if (isEmailUnique) {
                for {
                  _ <- userRepository.save(updatedUser)
                  _ <- eventPublisher.publishAll(updatedUser.getUncommittedEvents)
                } yield Right(())
              } else {
                Future.successful(Left(DomainError.ValidationError("Email already exists")))
              }
            } yield result
          case Left(error) =>
            Future.successful(Left(error))
        }
      case None =>
        Future.successful(Left(DomainError.NotFound(s"User not found: ${command.userId.value}")))
    }
  }

  def deleteUser(command: DeleteUserCommand): Future[Either[DomainError, Unit]] = {
    userRepository.findById(command.userId).flatMap {
      case Some(user) =>
        val deactivatedUser = user.deactivate()
        for {
          success <- userRepository.delete(command.userId)
          _ <- if (success) eventPublisher.publishAll(deactivatedUser.getUncommittedEvents) else Future.unit
        } yield if (success) Right(()) else Left(DomainError.InvalidOperation("Failed to delete user"))
      case None =>
        Future.successful(Left(DomainError.NotFound(s"User not found: ${command.userId.value}")))
    }
  }
}

/**
 * Query Service for User read operations
 */
@Singleton
class UserQueryService @Inject()(
  userRepository: UserRepository
)(implicit ec: ExecutionContext) {

  def findById(query: GetUserByIdQuery): Future[Option[UserDTO]] = {
    userRepository.findById(query.userId).map(_.map(UserDTO.fromDomain))
  }

  def findByEmail(query: GetUserByEmailQuery): Future[Option[UserDTO]] = {
    userRepository.findByEmail(query.email).map(_.map(UserDTO.fromDomain))
  }

  def findAll(query: GetAllUsersQuery): Future[Seq[UserDTO]] = {
    userRepository.findAll().map(_.map(UserDTO.fromDomain))
  }
}

// Event Publisher interface
trait EventPublisher {
  def publish(event: DomainEvent): Future[Unit]
  def publishAll(events: List[DomainEvent]): Future[Unit]
}