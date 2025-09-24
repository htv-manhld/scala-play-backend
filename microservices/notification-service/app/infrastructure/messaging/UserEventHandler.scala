package infrastructure.messaging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.JsValue
import application.notification.NotificationService
import application.notification.commands.SendNotificationCommand
import domain.notification.{UserId, NotificationChannel}

@Singleton
class UserEventHandler @Inject()(
  notificationService: NotificationService
)(implicit ec: ExecutionContext) extends EventHandler {

  override def handle(eventType: String, eventData: JsValue): Future[Unit] = {
    eventType match {
      case "UserCreated" =>
        handleUserCreated(eventData)
      case "UserEmailChanged" =>
        handleUserEmailChanged(eventData)
      case _ =>
        println(s"Unhandled event type: $eventType")
        Future.successful(())
    }
  }

  private def handleUserCreated(eventData: JsValue): Future[Unit] = {
    val userId = (eventData \ "userId" \ "value").as[Long].toString
    val email = (eventData \ "email" \ "value").as[String]
    val userName = (eventData \ "profile" \ "name").as[String]

    println(s"Handling UserCreated event for user: $userId")

    val command = SendNotificationCommand(
      userId = UserId(userId),
      channel = NotificationChannel.Email,
      subject = "Welcome to our service!",
      content = s"Hello $userName, thank you for signing up!",
      recipient = email
    )

    notificationService.handle(command).map {
      case Right(notification) =>
        println(s"Welcome notification sent to $email")
      case Left(error) =>
        println(s"Failed to send welcome notification: ${error.message}")
    }
  }

  private def handleUserEmailChanged(eventData: JsValue): Future[Unit] = {
    val userId = (eventData \ "userId" \ "value").as[Long].toString
    val newEmail = (eventData \ "newEmail" \ "value").as[String]

    println(s"Handling UserEmailChanged event for user: $userId")

    val command = SendNotificationCommand(
      userId = UserId(userId),
      channel = NotificationChannel.Email,
      subject = "Email Changed Confirmation",
      content = s"Your email has been changed to $newEmail",
      recipient = newEmail
    )

    notificationService.handle(command).map {
      case Right(notification) =>
        println(s"Email change notification sent to $newEmail")
      case Left(error) =>
        println(s"Failed to send email change notification: ${error.message}")
    }
  }
}