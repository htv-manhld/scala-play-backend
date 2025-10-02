package domain.user

import java.time.{LocalDate, LocalDateTime}
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

// User status enum
sealed trait UserStatus {
  def value: Int
}

object UserStatus {
  case object Inactive extends UserStatus { val value = 0 }
  case object Active extends UserStatus { val value = 1 }

  def fromInt(value: Int): UserStatus = value match {
    case 0 => Inactive
    case 1 => Active
    case _ => throw new IllegalArgumentException(s"Invalid status value: $value")
  }

  implicit val userStatusFormat: Format[UserStatus] = Format(
    Reads {
      case JsNumber(value) => JsSuccess(fromInt(value.toInt))
      case JsString("Active") => JsSuccess(Active)
      case JsString("Inactive") => JsSuccess(Inactive)
      case _ => JsError("Invalid user status")
    },
    Writes {
      case Active => JsNumber(1)
      case Inactive => JsNumber(0)
    }
  )
}

case class UserProfile(
  name: String,
  birthdate: Option[LocalDate] = None
) {
  require(name.nonEmpty, "Name cannot be empty")
  birthdate.foreach { bd =>
    require(bd.isBefore(LocalDate.now()), "Birthdate must be in the past")
  }
}

object UserProfile {
  implicit val localDateFormat: Format[LocalDate] = Format(
    Reads.of[String].map(LocalDate.parse),
    Writes.of[String].contramap(_.toString)
  )

  implicit val userProfileFormat: Format[UserProfile] = Json.format[UserProfile]
}

case class User(
  id: UserId,
  email: Email,
  password: Option[String], // Hashed password
  profile: UserProfile,
  status: UserStatus,
  lastLoginAt: Option[LocalDateTime] = None,
  verifiedAt: Option[LocalDateTime] = None,
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

  def activate(): User = {
    this.copy(
      status = UserStatus.Active,
      updatedAt = LocalDateTime.now(),
      version = version + 1
    )
  }

  def deactivate(): User = {
    this.copy(
      status = UserStatus.Inactive,
      updatedAt = LocalDateTime.now(),
      version = version + 1
    )
  }

  def verify(): User = {
    this.copy(
      verifiedAt = Some(LocalDateTime.now()),
      updatedAt = LocalDateTime.now(),
      version = version + 1
    )
  }

  def recordLogin(): User = {
    this.copy(
      lastLoginAt = Some(LocalDateTime.now()),
      updatedAt = LocalDateTime.now()
    )
  }

  def isActive: Boolean = status == UserStatus.Active
  def isVerified: Boolean = verifiedAt.isDefined
}

object User {
  def create(
    email: Email,
    profile: UserProfile,
    password: Option[String] = None,
    status: UserStatus = UserStatus.Active
  ): Either[DomainError, User] = {
    try {
      val now = LocalDateTime.now()
      val user = User(
        id = UserId(0), // Will be set by repository
        email = email,
        password = password,
        profile = profile,
        status = status,
        lastLoginAt = None,
        verifiedAt = None,
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