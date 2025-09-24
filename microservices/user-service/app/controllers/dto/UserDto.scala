package controllers.dto

import play.api.libs.json._
import domain.user.User
import java.time.LocalDateTime

// Response DTOs
case class UserResponseDto(
  id: String,
  email: String,
  name: String,
  age: Int,
  createdAt: String,
  updatedAt: String
)

object UserResponseDto {
  implicit val format: Format[UserResponseDto] = Json.format[UserResponseDto]

  def fromDomain(user: User): UserResponseDto = UserResponseDto(
    id = user.id.value.toString,
    email = user.email.value,
    name = user.profile.name,
    age = user.profile.age,
    createdAt = user.createdAt.toString,
    updatedAt = user.updatedAt.toString
  )
}

// Request DTOs
case class CreateUserRequestDto(
  email: String,
  name: String,
  age: Int
)

object CreateUserRequestDto {
  implicit val format: Format[CreateUserRequestDto] = Json.format[CreateUserRequestDto]
}

case class UpdateUserRequestDto(
  name: String,
  age: Int
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