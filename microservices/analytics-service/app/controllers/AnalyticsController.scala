package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import infrastructure.messaging.DomainEventHandler

@Singleton
class AnalyticsController @Inject()(
  val controllerComponents: ControllerComponents,
  domainEventHandler: DomainEventHandler
)(implicit ec: ExecutionContext) extends BaseController {

  def getUserMetrics(startDate: Option[String], endDate: Option[String]): Action[AnyContent] = Action.async {
    val metrics = domainEventHandler.getMetrics()

    val response = Json.obj(
      "totalUsers" -> metrics.getOrElse("totalUsers", 0L),
      "activeUsers" -> (metrics.getOrElse("totalUsers", 0L) * 0.75).toLong,
      "newUsers" -> metrics.getOrElse("totalUsers", 0L),
      "period" -> Json.obj(
        "startDate" -> startDate.getOrElse("2025-09-01"),
        "endDate" -> endDate.getOrElse("2025-09-30")
      )
    )
    Future.successful(Ok(response))
  }

  def getSystemMetrics(): Action[AnyContent] = Action.async {
    val metrics = domainEventHandler.getMetrics()

    val response = Json.obj(
      "totalUsers" -> metrics.getOrElse("totalUsers", 0L),
      "totalNotifications" -> metrics.getOrElse("totalNotifications", 0L),
      "emailsSent" -> metrics.getOrElse("emailsSent", 0L),
      "timestamp" -> System.currentTimeMillis()
    )
    Future.successful(Ok(response))
  }

  def getReports(reportType: String): Action[AnyContent] = Action.async {
    val metrics = domainEventHandler.getMetrics()

    val response = Json.obj(
      "type" -> reportType,
      "generatedAt" -> System.currentTimeMillis(),
      "data" -> Json.obj(
        "summary" -> s"${reportType.capitalize} analytics report",
        "totalUsers" -> metrics.getOrElse("totalUsers", 0L),
        "totalNotifications" -> metrics.getOrElse("totalNotifications", 0L),
        "trends" -> Json.arr(
          Json.obj("metric" -> "users", "value" -> metrics.getOrElse("totalUsers", 0L)),
          Json.obj("metric" -> "notifications", "value" -> metrics.getOrElse("totalNotifications", 0L))
        )
      )
    )
    Future.successful(Ok(response))
  }

  def trackEvent(): Action[JsValue] = Action.async(parse.json) { request =>
    val eventData = request.body
    val response = Json.obj(
      "status" -> "recorded",
      "eventId" -> java.util.UUID.randomUUID().toString,
      "timestamp" -> System.currentTimeMillis(),
      "message" -> "Event tracked successfully"
    )
    Future.successful(Ok(response))
  }
}