package application.notification

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import domain.notification.{Notification, NotificationId, UserId, NotificationRepository}
import domain.shared.DomainError
import infrastructure.messaging.EventPublisher
import application.shared.{ApplicationBase, LoggingService}
import application.notification.commands._
import application.notification.queries._

@Singleton
class NotificationService @Inject()(
  notificationRepository: NotificationRepository,
  eventPublisher: EventPublisher,
  loggingService: LoggingService
)(implicit ec: ExecutionContext) extends ApplicationBase {

  // Query operations
  def handle(query: GetNotificationByIdQuery): Future[Option[Notification]] = {
    loggingService.timed(s"GetNotificationByIdQuery", Some(query.notificationId.value)) {
      notificationRepository.findById(query.notificationId)
    }
  }

  def handle(query: GetUserNotificationsQuery): Future[Seq[Notification]] = {
    notificationRepository.findByUserId(query.userId, query.page, query.size)
  }

  // Command operations
  def handle(command: SendNotificationCommand): Future[Either[DomainError, Notification]] = {
    executeWithLogging("SendNotificationCommand", Map("userId" -> command.userId.value, "channel" -> command.channel.toString)) {
      Notification.create(
        command.userId,
        command.channel,
        command.subject,
        command.content,
        command.recipient
      ) match {
        case Right(notification) =>
          for {
            savedResult <- notificationRepository.save(notification)
            result <- savedResult match {
              case Right(savedNotification) =>
                // Mark as sent immediately after save
                val sentNotification = savedNotification.markAsSent()
                notificationRepository.save(sentNotification).flatMap {
                  case Right(finalNotification) =>
                    eventPublisher.publishAll(finalNotification.uncommittedEvents).map { _ =>
                      finalNotification.markEventsAsCommitted()
                      Right(finalNotification)
                    }
                  case Left(error) => Future.successful(Left(error))
                }
              case Left(error) => Future.successful(Left(error))
            }
          } yield result
        case Left(error) => Future.successful(Left(error))
      }
    }
  }

  def handle(command: MarkAsDeliveredCommand): Future[Either[DomainError, Notification]] = {
    for {
      notificationOpt <- notificationRepository.findById(command.notificationId)
      result <- notificationOpt match {
        case Some(notification) =>
          notification.markAsDelivered() match {
            case Right(deliveredNotification) =>
              for {
                saveResult <- notificationRepository.save(deliveredNotification)
                result <- saveResult match {
                  case Right(savedNotification) =>
                    eventPublisher.publishAll(savedNotification.uncommittedEvents).map { _ =>
                      savedNotification.markEventsAsCommitted()
                      Right(savedNotification)
                    }
                  case Left(error) => Future.successful(Left(error))
                }
              } yield result
            case Left(error) => Future.successful(Left(error))
          }
        case None =>
          Future.successful(Left(DomainError.NotFound(s"Notification with id ${command.notificationId.value} not found")))
      }
    } yield result
  }

  def handle(command: MarkAsFailedCommand): Future[Either[DomainError, Notification]] = {
    for {
      notificationOpt <- notificationRepository.findById(command.notificationId)
      result <- notificationOpt match {
        case Some(notification) =>
          val failedNotification = notification.markAsFailed(command.reason)
          for {
            saveResult <- notificationRepository.save(failedNotification)
            result <- saveResult match {
              case Right(savedNotification) =>
                eventPublisher.publishAll(savedNotification.uncommittedEvents).map { _ =>
                  savedNotification.markEventsAsCommitted()
                  Right(savedNotification)
                }
              case Left(error) => Future.successful(Left(error))
            }
          } yield result
        case None =>
          Future.successful(Left(DomainError.NotFound(s"Notification with id ${command.notificationId.value} not found")))
      }
    } yield result
  }
}