package domain.notification

import java.time.LocalDateTime
import domain.shared.{AggregateRoot, EntityId, DomainError}
import domain.notification.events._
import play.api.libs.json._

case class NotificationId(value: Long) extends EntityId[Long]

object NotificationId {
  implicit val format: Format[NotificationId] = Json.format[NotificationId]
}

case class UserId(value: String) {
  require(value.nonEmpty, "UserId cannot be empty")
}

object UserId {
  implicit val format: Format[UserId] = Json.format[UserId]
}

sealed trait NotificationChannel
object NotificationChannel {
  case object Email extends NotificationChannel
  case object Push extends NotificationChannel
  case object SMS extends NotificationChannel

  def fromString(s: String): Either[String, NotificationChannel] = s.toLowerCase match {
    case "email" => Right(Email)
    case "push" => Right(Push)
    case "sms" => Right(SMS)
    case _ => Left(s"Invalid notification channel: $s")
  }

  implicit val format: Format[NotificationChannel] = new Format[NotificationChannel] {
    def reads(json: JsValue): JsResult[NotificationChannel] = json match {
      case JsString("email") => JsSuccess(Email)
      case JsString("push") => JsSuccess(Push)
      case JsString("sms") => JsSuccess(SMS)
      case _ => JsError("Invalid notification channel")
    }
    def writes(channel: NotificationChannel): JsValue = channel match {
      case Email => JsString("email")
      case Push => JsString("push")
      case SMS => JsString("sms")
    }
  }
}

sealed trait NotificationStatus
object NotificationStatus {
  case object Pending extends NotificationStatus
  case object Sent extends NotificationStatus
  case object Delivered extends NotificationStatus
  case object Failed extends NotificationStatus

  implicit val format: Format[NotificationStatus] = new Format[NotificationStatus] {
    def reads(json: JsValue): JsResult[NotificationStatus] = json match {
      case JsString("pending") => JsSuccess(Pending)
      case JsString("sent") => JsSuccess(Sent)
      case JsString("delivered") => JsSuccess(Delivered)
      case JsString("failed") => JsSuccess(Failed)
      case _ => JsError("Invalid notification status")
    }
    def writes(status: NotificationStatus): JsValue = status match {
      case Pending => JsString("pending")
      case Sent => JsString("sent")
      case Delivered => JsString("delivered")
      case Failed => JsString("failed")
    }
  }
}

case class Notification(
  id: NotificationId,
  userId: UserId,
  channel: NotificationChannel,
  subject: String,
  content: String,
  status: NotificationStatus,
  recipient: String,
  attempts: Int = 0,
  sentAt: Option[LocalDateTime] = None,
  deliveredAt: Option[LocalDateTime] = None,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime,
  version: Long = 0
) extends AggregateRoot[NotificationId] {

  def markAsSent(): Notification = {
    val now = LocalDateTime.now()
    val events = List(NotificationSent(id, userId, now))
    this.copy(
      status = NotificationStatus.Sent,
      sentAt = Some(now),
      attempts = attempts + 1,
      updatedAt = now,
      version = version + 1
    ).addEvents(events)
  }

  def markAsDelivered(): Either[DomainError, Notification] = {
    status match {
      case NotificationStatus.Sent =>
        val now = LocalDateTime.now()
        val events = List(NotificationDelivered(id, userId, now))
        Right(this.copy(
          status = NotificationStatus.Delivered,
          deliveredAt = Some(now),
          updatedAt = now,
          version = version + 1
        ).addEvents(events))
      case _ => Left(DomainError.InvalidOperation("Notification must be sent before being marked as delivered"))
    }
  }

  def markAsFailed(reason: String): Notification = {
    val now = LocalDateTime.now()
    val events = List(NotificationFailed(id, userId, reason, now))
    this.copy(
      status = NotificationStatus.Failed,
      updatedAt = now,
      version = version + 1
    ).addEvents(events)
  }
}

object Notification {
  def create(
    userId: UserId,
    channel: NotificationChannel,
    subject: String,
    content: String,
    recipient: String
  ): Either[DomainError, Notification] = {
    try {
      require(subject.nonEmpty, "Subject cannot be empty")
      require(content.nonEmpty, "Content cannot be empty")
      require(recipient.nonEmpty, "Recipient cannot be empty")

      val now = LocalDateTime.now()
      val notification = Notification(
        id = NotificationId(0),
        userId = userId,
        channel = channel,
        subject = subject,
        content = content,
        status = NotificationStatus.Pending,
        recipient = recipient,
        createdAt = now,
        updatedAt = now
      )

      val event = NotificationCreated(notification.id, userId, channel, now)
      Right(notification.addEvent(event))
    } catch {
      case ex: IllegalArgumentException => Left(DomainError.ValidationError(ex.getMessage))
    }
  }

  implicit val localDateTimeFormat: Format[LocalDateTime] = Format(
    Reads.of[String].map(LocalDateTime.parse),
    Writes.of[String].contramap(_.toString)
  )

  implicit val format: Format[Notification] = Json.format[Notification]
}