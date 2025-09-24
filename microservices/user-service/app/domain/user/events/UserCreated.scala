package domain.user.events

import java.time.LocalDateTime
import domain.shared.{DomainEvent, EntityId}
import domain.user.{UserId, Email, UserProfile}
import play.api.libs.json._

case class UserCreated(
  userId: UserId,
  email: Email,
  profile: UserProfile,
  occurredAt: LocalDateTime
) extends DomainEvent {
  override def aggregateId: EntityId[_] = userId
}

object UserCreated {
  implicit val format: Format[UserCreated] = Json.format[UserCreated]
}