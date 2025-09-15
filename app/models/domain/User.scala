package models.domain

import play.api.libs.json.{Json, OFormat}

/**
 * User model shared between backend and frontend
 */
case class User(
  id: Long,
  name: String,
  email: String,
  age: Int,
  createdAt: Option[java.time.LocalDateTime] = None,
  updatedAt: Option[java.time.LocalDateTime] = None
)

object User {
  implicit val format: OFormat[User] = Json.format[User]
}

