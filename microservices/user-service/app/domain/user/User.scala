package domain.user

import java.time.{LocalDate, LocalDateTime}
import domain.shared.{AggregateRoot, EntityId, DomainError, DomainEvent}
import domain.user.events.{UserCreated, UserProfileChanged, UserEmailChanged}
import play.api.libs.json._

// Sealed trait to distinguish between new and persisted users
sealed trait UserId extends EntityId[Option[Long]] {
  def value: Option[Long]
  def isNew: Boolean
  def isPersisted: Boolean
}

case class NewUserId() extends UserId {
  override val value: Option[Long] = None
  override val isNew: Boolean = true
  override val isPersisted: Boolean = false
}

case class PersistedUserId(id: Long) extends UserId {
  override val value: Option[Long] = Some(id)
  override val isNew: Boolean = false
  override val isPersisted: Boolean = true
}

object UserId {
  def newUser(): UserId = NewUserId()
  def existing(id: Long): UserId = PersistedUserId(id)

  // For backward compatibility with repository queries
  def fromLong(id: Long): UserId = PersistedUserId(id)

  implicit val userIdFormat: Format[UserId] = Format(
    Reads {
      case JsNumber(value) => JsSuccess(PersistedUserId(value.toLong))
      case JsNull => JsSuccess(NewUserId())
      case _ => JsError("Invalid UserId format")
    },
    Writes {
      case NewUserId() => JsNull
      case PersistedUserId(id) => JsNumber(id)
    }
  )
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
  version: Long = 0,
  protected val _uncommittedEvents: List[DomainEvent] = List.empty
) extends AggregateRoot[UserId] {

  // Immutable event handling
  override def uncommittedEvents: List[DomainEvent] = _uncommittedEvents

  override protected def withEvent(event: DomainEvent): this.type =
    this.copy(_uncommittedEvents = _uncommittedEvents :+ event).asInstanceOf[this.type]

  override protected def withEvents(events: List[DomainEvent]): this.type =
    this.copy(_uncommittedEvents = _uncommittedEvents ++ events).asInstanceOf[this.type]

  override protected def withoutEvents(): this.type =
    this.copy(_uncommittedEvents = List.empty).asInstanceOf[this.type]

  def markEventsAsCommitted(): User = withoutEvents()

  // Business methods with immutable event handling
  def changeProfile(newProfile: UserProfile): User = {
    val event = UserProfileChanged(id, profile, newProfile, LocalDateTime.now())
    this.copy(
      profile = newProfile,
      updatedAt = LocalDateTime.now(),
      version = version + 1,
      _uncommittedEvents = _uncommittedEvents :+ event
    )
  }

  def changeEmail(newEmail: Email): Either[DomainError, User] = {
    if (newEmail != email) {
      val event = UserEmailChanged(id, email, newEmail, LocalDateTime.now())
      Right(this.copy(
        email = newEmail,
        updatedAt = LocalDateTime.now(),
        version = version + 1,
        _uncommittedEvents = _uncommittedEvents :+ event
      ))
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
  /**
   * Factory method for creating new users
   * Delegates to UserFactory for full validation
   */
  def create(
    email: Email,
    profile: UserProfile,
    password: Option[String] = None,
    status: UserStatus = UserStatus.Active
  ): Either[DomainError, User] = {
    UserFactory.createUser(email, profile, password, status)
  }

  /**
   * Reconstitute a User from persistence
   * Used by Repository when loading from database
   */
  def reconstitute(
    id: UserId,
    email: Email,
    password: Option[String],
    profile: UserProfile,
    status: UserStatus,
    lastLoginAt: Option[LocalDateTime],
    verifiedAt: Option[LocalDateTime],
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    version: Long
  ): User = {
    UserFactory.reconstitute(id, email, password, profile, status, lastLoginAt, verifiedAt, createdAt, updatedAt, version)
  }

  implicit val localDateTimeFormat: Format[LocalDateTime] = Format(
    Reads.of[String].map(LocalDateTime.parse),
    Writes.of[String].contramap(_.toString)
  )

  // Custom JSON format excluding uncommitted events
  implicit val userFormat: Format[User] = new Format[User] {
    override def reads(json: JsValue): JsResult[User] = {
      for {
        id <- (json \ "id").validate[UserId]
        email <- (json \ "email").validate[Email]
        password <- (json \ "password").validateOpt[String]
        profile <- (json \ "profile").validate[UserProfile]
        status <- (json \ "status").validate[UserStatus]
        lastLoginAt <- (json \ "lastLoginAt").validateOpt[LocalDateTime]
        verifiedAt <- (json \ "verifiedAt").validateOpt[LocalDateTime]
        createdAt <- (json \ "createdAt").validate[LocalDateTime]
        updatedAt <- (json \ "updatedAt").validate[LocalDateTime]
        version <- (json \ "version").validate[Long]
      } yield User(id, email, password, profile, status, lastLoginAt, verifiedAt, createdAt, updatedAt, version)
    }

    override def writes(user: User): JsValue = Json.obj(
      "id" -> user.id,
      "email" -> user.email,
      "password" -> user.password,
      "profile" -> user.profile,
      "status" -> user.status,
      "lastLoginAt" -> user.lastLoginAt,
      "verifiedAt" -> user.verifiedAt,
      "createdAt" -> user.createdAt,
      "updatedAt" -> user.updatedAt,
      "version" -> user.version
      // Excluding _uncommittedEvents from JSON
    )
  }
}