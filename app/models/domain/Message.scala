package models.domain

import play.api.libs.json.{Json, OFormat}

/**
 * Message model shared between backend and frontend
 */
case class Message(
  id: Option[Int] = None,
  text: String,
  userId: Option[Int] = None,
  timestamp: String,
  createdAt: Option[String] = None
)

object Message {
  implicit val format: OFormat[Message] = Json.format[Message]
}

