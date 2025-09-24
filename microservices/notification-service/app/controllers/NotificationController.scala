package controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc._
import play.api.libs.json._
import application.notification.NotificationService
import application.notification.commands._
import application.notification.queries._
import domain.notification.{NotificationId, UserId, NotificationChannel}
import controllers.dto._

@Singleton
class NotificationController @Inject()(
  cc: ControllerComponents,
  notificationService: NotificationService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def sendNotification(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[SendNotificationRequestDto] match {
      case JsSuccess(dto, _) =>
        NotificationChannel.fromString(dto.channel) match {
          case Right(channel) =>
            val command = SendNotificationCommand(
              userId = UserId(dto.userId),
              channel = channel,
              subject = dto.subject,
              content = dto.content,
              recipient = dto.recipient
            )

            notificationService.handle(command).map {
              case Right(notification) =>
                val notificationDto = NotificationResponseDto.fromDomain(notification)
                val response = Json.obj(
                  "success" -> true,
                  "data" -> notificationDto,
                  "message" -> "Notification sent successfully"
                )
                Created(response)
              case Left(error) =>
                val response = Json.obj(
                  "success" -> false,
                  "error" -> error.message,
                  "code" -> error.code
                )
                BadRequest(response)
            }
          case Left(error) =>
            val response = Json.obj(
              "success" -> false,
              "error" -> error,
              "code" -> "INVALID_CHANNEL"
            )
            Future.successful(BadRequest(response))
        }
      case JsError(errors) =>
        val errorMessage = errors.map { case (path, validationErrors) =>
          s"${path.toString()}: ${validationErrors.map(_.message).mkString(", ")}"
        }.mkString("; ")

        val response = Json.obj(
          "success" -> false,
          "error" -> s"Invalid request: $errorMessage",
          "code" -> "INVALID_REQUEST"
        )
        Future.successful(BadRequest(response))
    }
  }

  def getUserNotifications(userId: String): Action[AnyContent] = Action.async { implicit request =>
    val query = GetUserNotificationsQuery(UserId(userId))

    notificationService.handle(query).map { notifications =>
      val notificationsDto = notifications.map(NotificationResponseDto.fromDomain)

      val response = Json.obj(
        "success" -> true,
        "data" -> notificationsDto,
        "totalCount" -> notificationsDto.size,
        "message" -> "Notifications retrieved successfully"
      )
      Ok(response)
    }.recover {
      case ex: Exception =>
        val response = Json.obj(
          "success" -> false,
          "error" -> ex.getMessage,
          "code" -> "INTERNAL_ERROR"
        )
        InternalServerError(response)
    }
  }

  def getNotificationStatus(id: String): Action[AnyContent] = Action.async { implicit request =>
    val query = GetNotificationByIdQuery(NotificationId(id.toLong))

    notificationService.handle(query).map {
      case Some(notification) =>
        val notificationDto = NotificationResponseDto.fromDomain(notification)

        val response = Json.obj(
          "success" -> true,
          "data" -> notificationDto,
          "message" -> "Notification status retrieved successfully"
        )
        Ok(response)
      case None =>
        val response = Json.obj(
          "success" -> false,
          "error" -> "Notification not found",
          "code" -> "NOT_FOUND"
        )
        NotFound(response)
    }.recover {
      case ex: Exception =>
        val response = Json.obj(
          "success" -> false,
          "error" -> ex.getMessage,
          "code" -> "INTERNAL_ERROR"
        )
        InternalServerError(response)
    }
  }

  def markAsDelivered(id: String): Action[AnyContent] = Action.async { implicit request =>
    val command = MarkAsDeliveredCommand(NotificationId(id.toLong))

    notificationService.handle(command).map {
      case Right(notification) =>
        val notificationDto = NotificationResponseDto.fromDomain(notification)

        val response = Json.obj(
          "success" -> true,
          "data" -> notificationDto,
          "message" -> "Notification marked as delivered"
        )
        Ok(response)
      case Left(error) =>
        val response = Json.obj(
          "success" -> false,
          "error" -> error.message,
          "code" -> error.code
        )
        error.code match {
          case "NOT_FOUND" => NotFound(response)
          case _ => BadRequest(response)
        }
    }
  }

  def markAsFailed(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    (request.body \ "reason").asOpt[String] match {
      case Some(reason) =>
        val command = MarkAsFailedCommand(NotificationId(id.toLong), reason)

        notificationService.handle(command).map {
          case Right(notification) =>
            val notificationDto = NotificationResponseDto.fromDomain(notification)

            val response = Json.obj(
              "success" -> true,
              "data" -> notificationDto,
              "message" -> "Notification marked as failed"
            )
            Ok(response)
          case Left(error) =>
            val response = Json.obj(
              "success" -> false,
              "error" -> error.message,
              "code" -> error.code
            )
            error.code match {
              case "NOT_FOUND" => NotFound(response)
              case _ => BadRequest(response)
            }
        }
      case None =>
        val response = Json.obj(
          "success" -> false,
          "error" -> "Missing required field: reason",
          "code" -> "INVALID_REQUEST"
        )
        Future.successful(BadRequest(response))
    }
  }
}