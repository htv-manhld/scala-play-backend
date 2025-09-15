package models.domain

import play.api.libs.json.{Json, OFormat}

/**
 * Request DTO for creating a new user
 */
case class UserCreateRequest(
  name: String,
  email: String,
  age: Int
)

object UserCreateRequest {
  implicit val format: OFormat[UserCreateRequest] = Json.format[UserCreateRequest]
}

