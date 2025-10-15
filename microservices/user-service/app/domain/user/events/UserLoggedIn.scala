package domain.user.events

import java.time.LocalDateTime
import domain.shared.{DomainEvent, EntityId}
import domain.user.UserId
import play.api.libs.json._

case class UserLoggedIn(
  userId: UserId,
  occurredAt: LocalDateTime
) extends DomainEvent {
  override def aggregateId: EntityId[_] = userId
}

object UserLoggedIn {
  implicit val format: Format[UserLoggedIn] = Json.format[UserLoggedIn]
}
