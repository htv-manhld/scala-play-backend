package domain.user.events

import java.time.LocalDateTime
import domain.shared.{DomainEvent, EntityId}
import domain.user.{UserId, Email}
import play.api.libs.json._

case class UserEmailChanged(
  userId: UserId,
  oldEmail: Email,
  newEmail: Email,
  occurredAt: LocalDateTime
) extends DomainEvent {
  override def aggregateId: EntityId[_] = userId
}

object UserEmailChanged {
  implicit val format: Format[UserEmailChanged] = Json.format[UserEmailChanged]
}