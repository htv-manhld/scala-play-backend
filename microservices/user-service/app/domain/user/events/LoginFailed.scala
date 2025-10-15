package domain.user.events

import java.time.LocalDateTime
import domain.shared.{DomainEvent, EntityId}
import domain.user.Email
import play.api.libs.json._

// Wrapper to make Email compatible with EntityId
case class EmailId(email: Email) extends EntityId[String] {
  override def value: String = email.value
}

case class LoginFailed(
  email: Email,
  reason: String,
  occurredAt: LocalDateTime
) extends DomainEvent {
  override def aggregateId: EntityId[_] = EmailId(email)
}

object LoginFailed {
  implicit val format: Format[LoginFailed] = Json.format[LoginFailed]
}
