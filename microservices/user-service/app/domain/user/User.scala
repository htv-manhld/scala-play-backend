package domain.user

import java.time.LocalDateTime
import domain.shared.{AggregateRoot, EntityId, DomainError}
import domain.user.events.{UserCreated, UserProfileChanged, UserEmailChanged}
import play.api.libs.json._

case class UserId(value: Long) extends EntityId[Long]

object UserId {
  implicit val userIdFormat: Format[UserId] = Json.format[UserId]
}

case class Email(value: String) {
  require(Email.isValid(value), s"Invalid email format: $value")
}

object Email {
  private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".r

  def isValid(email: String): Boolean = emailRegex.matches(email)

  implicit val emailFormat: Format[Email] = Json.format[Email]
}

case class UserProfile(
  name: String,
  age: Int
) {
  require(name.nonEmpty, "Name cannot be empty")
  require(age > 0 && age < 150, "Age must be between 1 and 149")
}

object UserProfile {
  implicit val userProfileFormat: Format[UserProfile] = Json.format[UserProfile]
}

case class User(
  id: UserId,
  email: Email,
  profile: UserProfile,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime,
  version: Long = 0
) extends AggregateRoot[UserId] {

  def changeProfile(newProfile: UserProfile): User = {
    val events = List(UserProfileChanged(id, profile, newProfile, LocalDateTime.now()))
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
      Left(DomainError.InvalidOperation("Email is the same as current email"))
    }
  }
}

object User {
  def create(email: Email, profile: UserProfile): Either[DomainError, User] = {
    try {
      val now = LocalDateTime.now()
      val user = User(
        id = UserId(0), // Will be set by repository
        email = email,
        profile = profile,
        createdAt = now,
        updatedAt = now,
        version = 0
      )

      val event = UserCreated(user.id, email, profile, now)
      val userWithEvent = user.addEvent(event)

      Right(userWithEvent)
    } catch {
      case ex: IllegalArgumentException => Left(DomainError.ValidationError(ex.getMessage))
    }
  }

  implicit val localDateTimeFormat: Format[LocalDateTime] = Format(
    Reads.of[String].map(LocalDateTime.parse),
    Writes.of[String].contramap(_.toString)
  )

  implicit val userFormat: Format[User] = Json.format[User]
}