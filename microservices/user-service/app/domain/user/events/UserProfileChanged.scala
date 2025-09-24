package domain.user.events

import java.time.LocalDateTime
import domain.shared.{DomainEvent, EntityId}
import domain.user.{UserId, UserProfile}
import play.api.libs.json._

case class UserProfileChanged(
  userId: UserId,
  oldProfile: UserProfile,
  newProfile: UserProfile,
  occurredAt: LocalDateTime
) extends DomainEvent {
  override def aggregateId: EntityId[_] = userId
}

object UserProfileChanged {
  implicit val format: Format[UserProfileChanged] = Json.format[UserProfileChanged]
}