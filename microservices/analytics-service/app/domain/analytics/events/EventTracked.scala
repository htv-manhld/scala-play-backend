package domain.analytics.events

import java.time.LocalDateTime
import domain.shared.{DomainEvent, EntityId}
import domain.analytics.{EventId, UserId}
import play.api.libs.json._

case class EventTracked(
  eventId: EventId,
  userId: Option[UserId],
  eventType: String,
  occurredAt: LocalDateTime
) extends DomainEvent {
  override def aggregateId: EntityId[_] = eventId
}

object EventTracked {
  implicit val format: Format[EventTracked] = Json.format[EventTracked]
}