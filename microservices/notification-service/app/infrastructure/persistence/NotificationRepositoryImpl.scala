package infrastructure.persistence

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._
import java.time.LocalDateTime
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import domain.notification._
import domain.shared.DomainError

@Singleton
class NotificationRepositoryImpl @Inject()(
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) extends NotificationRepository {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._

  class NotificationsTable(tag: Tag) extends Table[NotificationRow](tag, "notifications") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[String]("user_id")
    def channel = column[String]("channel")
    def subject = column[String]("subject")
    def content = column[String]("content")
    def status = column[String]("status")
    def recipient = column[String]("recipient")
    def attempts = column[Int]("attempts")
    def sentAt = column[Option[LocalDateTime]]("sent_at")
    def deliveredAt = column[Option[LocalDateTime]]("delivered_at")
    def createdAt = column[LocalDateTime]("created_at")
    def updatedAt = column[LocalDateTime]("updated_at")

    def * = (id, userId, channel, subject, content, status, recipient, attempts, sentAt, deliveredAt, createdAt, updatedAt).mapTo[NotificationRow]
  }

  private val notifications = TableQuery[NotificationsTable]

  case class NotificationRow(
    id: Long,
    userId: String,
    channel: String,
    subject: String,
    content: String,
    status: String,
    recipient: String,
    attempts: Int,
    sentAt: Option[LocalDateTime],
    deliveredAt: Option[LocalDateTime],
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime
  )

  private def toDomain(row: NotificationRow): Notification = {
    val channel = row.channel.toLowerCase match {
      case "email" => NotificationChannel.Email
      case "push" => NotificationChannel.Push
      case "sms" => NotificationChannel.SMS
      case _ => NotificationChannel.Email
    }

    val status = row.status.toLowerCase match {
      case "pending" => NotificationStatus.Pending
      case "sent" => NotificationStatus.Sent
      case "delivered" => NotificationStatus.Delivered
      case "failed" => NotificationStatus.Failed
      case _ => NotificationStatus.Pending
    }

    Notification(
      id = NotificationId(row.id),
      userId = UserId(row.userId),
      channel = channel,
      subject = row.subject,
      content = row.content,
      status = status,
      recipient = row.recipient,
      attempts = row.attempts,
      sentAt = row.sentAt,
      deliveredAt = row.deliveredAt,
      createdAt = row.createdAt,
      updatedAt = row.updatedAt,
      version = 0
    )
  }

  private def toRow(notification: Notification): NotificationRow = {
    val channelStr = notification.channel match {
      case NotificationChannel.Email => "email"
      case NotificationChannel.Push => "push"
      case NotificationChannel.SMS => "sms"
    }

    val statusStr = notification.status match {
      case NotificationStatus.Pending => "pending"
      case NotificationStatus.Sent => "sent"
      case NotificationStatus.Delivered => "delivered"
      case NotificationStatus.Failed => "failed"
    }

    NotificationRow(
      id = notification.id.value,
      userId = notification.userId.value,
      channel = channelStr,
      subject = notification.subject,
      content = notification.content,
      status = statusStr,
      recipient = notification.recipient,
      attempts = notification.attempts,
      sentAt = notification.sentAt,
      deliveredAt = notification.deliveredAt,
      createdAt = notification.createdAt,
      updatedAt = notification.updatedAt
    )
  }

  override def findById(id: NotificationId): Future[Option[Notification]] = {
    val query = notifications.filter(_.id === id.value)
    db.run(query.result.headOption).map(_.map(toDomain)).recover {
      case _ => None
    }
  }

  override def findByUserId(userId: UserId, page: Int = 0, size: Int = 20): Future[Seq[Notification]] = {
    val query = notifications
      .filter(_.userId === userId.value)
      .sortBy(_.createdAt.desc)
      .drop(page * size)
      .take(size)

    db.run(query.result).map(_.map(toDomain)).recover {
      case _ => Seq.empty
    }
  }

  override def findAll(page: Int = 0, size: Int = 20): Future[Seq[Notification]] = {
    val query = notifications
      .sortBy(_.id.asc)
      .drop(page * size)
      .take(size)

    db.run(query.result).map(_.map(toDomain)).recover {
      case _ => Seq.empty
    }
  }

  override def save(notification: Notification): Future[Either[DomainError, Notification]] = {
    if (notification.id.value == 0) {
      val insertAction = (notifications returning notifications.map(_.id)) += toRow(notification).copy(id = 0L)

      db.run(insertAction).map { generatedId =>
        val savedNotification = notification.copy(id = NotificationId(generatedId))
        Right(savedNotification)
      }.recover {
        case ex: Exception =>
          Left(DomainError.ValidationError(s"Database error: ${ex.getMessage}"))
      }
    } else {
      val updateAction = notifications
        .filter(_.id === notification.id.value)
        .map(n => (n.status, n.attempts, n.sentAt, n.deliveredAt, n.updatedAt))
        .update((
          toRow(notification).status,
          notification.attempts,
          notification.sentAt,
          notification.deliveredAt,
          LocalDateTime.now()
        ))

      db.run(updateAction).flatMap { rowsAffected =>
        if (rowsAffected > 0) {
          findById(notification.id).map {
            case Some(updatedNotification) => Right(updatedNotification)
            case None => Left(DomainError.NotFound(s"Notification with id ${notification.id.value} not found"))
          }
        } else {
          Future.successful(Left(DomainError.NotFound(s"Notification with id ${notification.id.value} not found")))
        }
      }.recover {
        case ex: Exception =>
          Left(DomainError.ValidationError(s"Database error: ${ex.getMessage}"))
      }
    }
  }

  override def delete(id: NotificationId): Future[Either[DomainError, Unit]] = {
    val deleteAction = notifications.filter(_.id === id.value).delete

    db.run(deleteAction).map { rowsAffected =>
      if (rowsAffected > 0) {
        Right(())
      } else {
        Left(DomainError.NotFound(s"Notification with id ${id.value} not found"))
      }
    }.recover {
      case ex: Exception =>
        Left(DomainError.ValidationError(s"Database error: ${ex.getMessage}"))
    }
  }

  override def nextIdentity(): Future[NotificationId] = {
    Future.successful(NotificationId(0L))
  }
}