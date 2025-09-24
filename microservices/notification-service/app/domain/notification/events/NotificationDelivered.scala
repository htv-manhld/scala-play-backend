package domain.notification.events

import java.time.LocalDateTime
import domain.shared.{DomainEvent, EntityId}
import domain.notification.{NotificationId, UserId}
import play.api.libs.json._

case class NotificationDelivered(
  notificationId: NotificationId,
  userId: UserId,
  occurredAt: LocalDateTime
) extends DomainEvent {
  override def aggregateId: EntityId[_] = notificationId
}

object NotificationDelivered {
  implicit val format: Format[NotificationDelivered] = Json.format[NotificationDelivered]
}