package domain.analytics

import java.time.LocalDateTime
import domain.shared.{AggregateRoot, EntityId, DomainError}
import domain.analytics.events._
import play.api.libs.json._

case class EventId(value: Long) extends EntityId[Long]

object EventId {
  implicit val format: Format[EventId] = Json.format[EventId]
}

case class UserId(value: String) {
  require(value.nonEmpty, "UserId cannot be empty")
}

object UserId {
  implicit val format: Format[UserId] = Json.format[UserId]
}

case class EventData(
  eventType: String,
  properties: Map[String, String]
)

object EventData {
  implicit val format: Format[EventData] = Json.format[EventData]
}

case class Event(
  id: EventId,
  userId: Option[UserId],
  eventType: String,
  eventData: EventData,
  createdAt: LocalDateTime,
  version: Long = 0
) extends AggregateRoot[EventId]

object Event {
  def track(
    userId: Option[UserId],
    eventType: String,
    properties: Map[String, String]
  ): Either[DomainError, Event] = {
    try {
      require(eventType.nonEmpty, "Event type cannot be empty")

      val now = LocalDateTime.now()
      val event = Event(
        id = EventId(0),
        userId = userId,
        eventType = eventType,
        eventData = EventData(eventType, properties),
        createdAt = now
      )

      val domainEvent = EventTracked(event.id, userId, eventType, now)
      Right(event.addEvent(domainEvent))
    } catch {
      case ex: IllegalArgumentException => Left(DomainError.ValidationError(ex.getMessage))
    }
  }

  implicit val localDateTimeFormat: Format[LocalDateTime] = Format(
    Reads.of[String].map(LocalDateTime.parse),
    Writes.of[String].contramap(_.toString)
  )

  implicit val format: Format[Event] = Json.format[Event]
}