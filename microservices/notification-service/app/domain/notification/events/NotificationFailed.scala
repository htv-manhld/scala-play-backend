package domain.notification.events

import java.time.LocalDateTime
import domain.shared.{DomainEvent, EntityId}
import domain.notification.{NotificationId, UserId}
import play.api.libs.json._

case class NotificationFailed(
  notificationId: NotificationId,
  userId: UserId,
  reason: String,
  occurredAt: LocalDateTime
) extends DomainEvent {
  override def aggregateId: EntityId[_] = notificationId
}

object NotificationFailed {
  implicit val format: Format[NotificationFailed] = Json.format[NotificationFailed]
}