package controllers.dto

import play.api.libs.json._
import domain.notification.Notification

case class NotificationResponseDto(
  id: String,
  userId: String,
  channel: String,
  subject: String,
  content: String,
  status: String,
  recipient: String,
  attempts: Int,
  sentAt: Option[String],
  deliveredAt: Option[String],
  createdAt: String,
  updatedAt: String
)

object NotificationResponseDto {
  implicit val format: Format[NotificationResponseDto] = Json.format[NotificationResponseDto]

  def fromDomain(notification: Notification): NotificationResponseDto = NotificationResponseDto(
    id = notification.id.value.toString,
    userId = notification.userId.value,
    channel = notification.channel.toString.toLowerCase,
    subject = notification.subject,
    content = notification.content,
    status = notification.status.toString.toLowerCase,
    recipient = notification.recipient,
    attempts = notification.attempts,
    sentAt = notification.sentAt.map(_.toString),
    deliveredAt = notification.deliveredAt.map(_.toString),
    createdAt = notification.createdAt.toString,
    updatedAt = notification.updatedAt.toString
  )
}

case class SendNotificationRequestDto(
  userId: String,
  channel: String,
  subject: String,
  content: String,
  recipient: String
)

object SendNotificationRequestDto {
  implicit val format: Format[SendNotificationRequestDto] = Json.format[SendNotificationRequestDto]
}