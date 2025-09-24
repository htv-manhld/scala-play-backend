package domain.notification.events

import java.time.LocalDateTime
import domain.shared.{DomainEvent, EntityId}
import domain.notification.{NotificationId, UserId}
import play.api.libs.json._

case class NotificationSent(
  notificationId: NotificationId,
  userId: UserId,
  occurredAt: LocalDateTime
) extends DomainEvent {
  override def aggregateId: EntityId[_] = notificationId
}

object NotificationSent {
  implicit val format: Format[NotificationSent] = Json.format[NotificationSent]
}