package domain.user

import java.time.LocalDateTime
import domain.shared.{AggregateRoot, DomainEvent, EntityId}

case class UserId(value: Long) extends EntityId[Long]

case class User(
  id: UserId,
  email: Email,
  profile: UserProfile,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime,
  version: Long = 0
) extends AggregateRoot[UserId] {

  def changeProfile(newProfile: UserProfile): User = {
    val events = List(UserProfileChanged(id, newProfile, LocalDateTime.now()))
    this.copy(
      profile = newProfile,
      updatedAt = LocalDateTime.now(),
      version = version + 1
    ).addEvents(events)
  }

  def changeEmail(newEmail: Email): Either[DomainError, User] = {
    if (newEmail != email) {
      val events = List(UserEmailChanged(id, email, newEmail, LocalDateTime.now()))
      Right(this.copy(
        email = newEmail,
        updatedAt = LocalDateTime.now(),
        version = version + 1
      ).addEvents(events))
    } else {
      Left(DomainError.InvalidOperation("Email is the same"))
    }
  }

  def deactivate(): User = {
    val events = List(UserDeactivated(id, LocalDateTime.now()))
    this.copy(
      updatedAt = LocalDateTime.now(),
      version = version + 1
    ).addEvents(events)
  }
}

case class UserProfile(
  name: String,
  age: Int
) {
  require(name.nonEmpty, "Name cannot be empty")
  require(age > 0 && age < 150, "Age must be between 1 and 149")
}

case class Email(value: String) {
  require(Email.isValid(value), s"Invalid email format: $value")
}

object Email {
  private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".r

  def isValid(email: String): Boolean = emailRegex.matches(email)

  def apply(value: String): Email = new Email(value)
}

// Domain Events
sealed trait UserDomainEvent extends DomainEvent {
  def userId: UserId
  def occurredAt: LocalDateTime
}

case class UserCreated(
  userId: UserId,
  email: Email,
  profile: UserProfile,
  occurredAt: LocalDateTime
) extends UserDomainEvent

case class UserProfileChanged(
  userId: UserId,
  newProfile: UserProfile,
  occurredAt: LocalDateTime
) extends UserDomainEvent

case class UserEmailChanged(
  userId: UserId,
  oldEmail: Email,
  newEmail: Email,
  occurredAt: LocalDateTime
) extends UserDomainEvent

case class UserDeactivated(
  userId: UserId,
  occurredAt: LocalDateTime
) extends UserDomainEvent

// Domain Errors
sealed trait DomainError {
  def message: String
}

object DomainError {
  case class ValidationError(message: String) extends DomainError
  case class NotFound(message: String) extends DomainError
  case class InvalidOperation(message: String) extends DomainError
}