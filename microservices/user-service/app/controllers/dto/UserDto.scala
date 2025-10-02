package controllers.dto

import play.api.libs.json._
import domain.user.{User, UserStatus}
import java.time.LocalDateTime

// Response DTOs
case class UserResponseDto(
  id: String,
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
  implicit val format: Format[UserResponseDto] = Json.format[UserResponseDto]

  def fromDomain(user: User): UserResponseDto = UserResponseDto(
    id = user.id.value.toString,
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