package models.domain

import play.api.libs.json.{Json, OFormat}

/**
 * Request DTO for updating a user
 */
case class UserUpdateRequest(
  name: Option[String] = None,
  email: Option[String] = None,
  age: Option[Int] = None
)

object UserUpdateRequest {
  implicit val format: OFormat[UserUpdateRequest] = Json.format[UserUpdateRequest]
}

