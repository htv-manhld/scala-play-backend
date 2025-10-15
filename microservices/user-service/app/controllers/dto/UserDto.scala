package controllers.dto

import play.api.libs.json._
import domain.user.{User, UserStatus}
import java.time.LocalDateTime

// Response DTOs
case class UserResponseDto(
  id: Long,  // Changed to Long instead of String
  email: String,
  name: String,
  birthdate: Option[String],
  status: Int,
  lastLoginAt: Option[String],
  verifiedAt: Option[String],
  createdAt: String,
  updatedAt: String
)

object UserResponseDto {
  // Explicit Writes to avoid any serialization issues
  implicit val writes: Writes[UserResponseDto] = Json.writes[UserResponseDto]
  implicit val reads: Reads[UserResponseDto] = Json.reads[UserResponseDto]

  def fromDomain(user: User): UserResponseDto = UserResponseDto(
    id = user.id.value.getOrElse(
      throw new IllegalStateException("User.id should not be None for persisted users")
    ),  // Return Long directly, not String
    email = user.email.value,
    name = user.profile.name,
    birthdate = user.profile.birthdate.map(_.toString),
    status = user.status.value, // Return raw DB value: 0 or 1
    lastLoginAt = user.lastLoginAt.map(_.toString),
    verifiedAt = user.verifiedAt.map(_.toString),
    createdAt = user.createdAt.toString,
    updatedAt = user.updatedAt.toString
  )
}

// Request DTOs
case class CreateUserRequestDto(
  email: String,
  name: String,
  password: Option[String] = None,
  birthdate: Option[String] = None
)

object CreateUserRequestDto {
  implicit val format: Format[CreateUserRequestDto] = Json.format[CreateUserRequestDto]
}

case class UpdateUserRequestDto(
  name: String,
  birthdate: Option[String] = None
)

object UpdateUserRequestDto {
  implicit val format: Format[UpdateUserRequestDto] = Json.format[UpdateUserRequestDto]
}

case class ChangeEmailRequestDto(
  email: String
)

object ChangeEmailRequestDto {
  implicit val format: Format[ChangeEmailRequestDto] = Json.format[ChangeEmailRequestDto]
}

// Authentication DTOs
case class LoginRequestDto(
  email: String,
  password: String
)

object LoginRequestDto {
  implicit val format: Format[LoginRequestDto] = Json.format[LoginRequestDto]
}

case class LoginResponseDto(
  user: UserResponseDto,
  token: String,
  expiresAt: String
)

object LoginResponseDto {
  implicit val format: Format[LoginResponseDto] = Json.format[LoginResponseDto]
}

case class ChangePasswordRequestDto(
  oldPassword: String,
  newPassword: String
)

object ChangePasswordRequestDto {
  implicit val format: Format[ChangePasswordRequestDto] = Json.format[ChangePasswordRequestDto]
}

case class ForgotPasswordRequestDto(
  email: String,
  resetUrl: Option[String] = None
)

object ForgotPasswordRequestDto {
  implicit val format: Format[ForgotPasswordRequestDto] = Json.format[ForgotPasswordRequestDto]
}

case class ResetPasswordRequestDto(
  token: String,
  newPassword: String
)

object ResetPasswordRequestDto {
  implicit val format: Format[ResetPasswordRequestDto] = Json.format[ResetPasswordRequestDto]
}
