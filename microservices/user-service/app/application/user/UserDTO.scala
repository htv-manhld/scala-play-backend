package application.user

import domain.user.User
import play.api.libs.json.{Json, OFormat}
import java.time.LocalDateTime

/**
 * Data Transfer Object for User
 * Used to transfer data between application and interface layers
 */
case class UserDTO(
  id: Long,
  name: String,
  email: String,
  age: Int,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime
)

object UserDTO {
  implicit val format: OFormat[UserDTO] = Json.format[UserDTO]

  def fromDomain(user: User): UserDTO = UserDTO(
    id = user.id.value,
    name = user.profile.name,
    email = user.email.value,
    age = user.profile.age,
    createdAt = user.createdAt,
    updatedAt = user.updatedAt
  )
}

// Request/Response DTOs for API
case class CreateUserRequest(
  name: String,
  email: String,
  age: Int
)

object CreateUserRequest {
  implicit val format: OFormat[CreateUserRequest] = Json.format[CreateUserRequest]
}

case class UpdateUserRequest(
  name: Option[String] = None,
  age: Option[Int] = None
)

object UpdateUserRequest {
  implicit val format: OFormat[UpdateUserRequest] = Json.format[UpdateUserRequest]
}

case class ChangeEmailRequest(
  email: String
)

object ChangeEmailRequest {
  implicit val format: OFormat[ChangeEmailRequest] = Json.format[ChangeEmailRequest]
}