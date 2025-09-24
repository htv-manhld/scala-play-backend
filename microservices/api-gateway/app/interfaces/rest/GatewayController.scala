package interfaces.rest

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc._
import play.api.libs.ws.WSClient
import play.api.Configuration
import play.api.libs.ws.WSBodyWritables.*

/**
 * API Gateway Controller - Routes requests to appropriate microservices
 */
@Singleton
class GatewayController @Inject()(
  cc: ControllerComponents,
  ws: WSClient,
  config: Configuration
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val userServiceUrl = config.get[String]("services.user-service.url")
  private val notificationServiceUrl = config.get[String]("services.notification-service.url")
  private val analyticsServiceUrl = config.get[String]("services.analytics-service.url")

  // Proxy all user-related requests to user-service
  def proxyToUserService(path: String): Action[AnyContent] = Action.async { request =>
    val url = s"$userServiceUrl/$path"
    proxyRequest(url, request)
  }

  // Proxy all notification-related requests
  def proxyToNotificationService(path: String): Action[AnyContent] = Action.async { request =>
    val url = s"$notificationServiceUrl/$path"
    proxyRequest(url, request)
  }

  // Proxy all analytics-related requests
  def proxyToAnalyticsService(path: String): Action[AnyContent] = Action.async { request =>
    val url = s"$analyticsServiceUrl/$path"
    proxyRequest(url, request)
  }

  private def proxyRequest(url: String, request: Request[AnyContent]): Future[Result] = {
    val wsRequest = ws.url(url)
      .withHttpHeaders(request.headers.headers: _*)
      .withQueryStringParameters(request.queryString.map { case (k, v) => k -> v.head }.toSeq: _*)

    val wsRequestWithBody = request.body.asJson match {
      case Some(json) => wsRequest.withBody(json)
      case None => wsRequest
    }

    val method = request.method.toUpperCase
    val futureResponse = method match {
      case "GET" => wsRequestWithBody.get()
      case "POST" => wsRequestWithBody.post(request.body.asJson.getOrElse(play.api.libs.json.Json.obj()))
      case "PUT" => wsRequestWithBody.put(request.body.asJson.getOrElse(play.api.libs.json.Json.obj()))
      case "DELETE" => wsRequestWithBody.delete()
      case "PATCH" => wsRequestWithBody.patch(request.body.asJson.getOrElse(play.api.libs.json.Json.obj()))
      case _ => wsRequestWithBody.get()
    }

    futureResponse.map { response =>
      Status(response.status)(response.body)
        .withHeaders(response.headers.map { case (k, v) => k -> v.head }.toSeq: _*)
        .as(response.contentType)
    }
  }
}