package domain.notification.events

import java.time.LocalDateTime
import domain.shared.{DomainEvent, EntityId}
import domain.notification.{NotificationId, UserId, NotificationChannel}
import play.api.libs.json._

case class NotificationCreated(
  notificationId: NotificationId,
  userId: UserId,
  channel: NotificationChannel,
  occurredAt: LocalDateTime
) extends DomainEvent {
  override def aggregateId: EntityId[_] = notificationId
}

object NotificationCreated {
  implicit val format: Format[NotificationCreated] = Json.format[NotificationCreated]
}