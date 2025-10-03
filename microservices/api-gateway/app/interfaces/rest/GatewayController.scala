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
  def proxyToUserService(path: String = ""): Action[AnyContent] = Action.async { request =>
    val fullPath = if (path.isEmpty) "api/users" else s"api/users/$path"
    val url = s"$userServiceUrl/$fullPath"
    proxyRequest(url, request)
  }

  // Proxy all notification-related requests
  def proxyToNotificationService(path: String = ""): Action[AnyContent] = Action.async { request =>
    val fullPath = if (path.isEmpty) "api/notifications" else s"api/notifications/$path"
    val url = s"$notificationServiceUrl/$fullPath"
    proxyRequest(url, request)
  }

  // Proxy all analytics-related requests
  def proxyToAnalyticsService(path: String = ""): Action[AnyContent] = Action.async { request =>
    val fullPath = if (path.isEmpty) "api/analytics" else s"api/analytics/$path"
    val url = s"$analyticsServiceUrl/$fullPath"
    proxyRequest(url, request)
  }

  private def proxyRequest(url: String, request: Request[AnyContent]): Future[Result] = {
    // Filter out headers that should not be forwarded
    val headersToExclude = Set("Host", "Content-Length", "Content-Type", "Transfer-Encoding")
    val filteredHeaders = request.headers.headers.filterNot { case (name, _) =>
      headersToExclude.contains(name)
    }

    val wsRequest = ws.url(url)
      .withHttpHeaders(filteredHeaders*)
      .withQueryStringParameters(request.queryString.map { case (k, v) => k -> v.head }.toSeq*)
      .withRequestTimeout(scala.concurrent.duration.Duration(30, "seconds"))

    val method = request.method.toUpperCase
    val futureResponse = method match {
      case "GET" => wsRequest.get()
      case "POST" =>
        request.body.asJson match {
          case Some(json) => wsRequest.post(json)
          case None => wsRequest.post("")
        }
      case "PUT" =>
        request.body.asJson match {
          case Some(json) => wsRequest.put(json)
          case None => wsRequest.put("")
        }
      case "DELETE" => wsRequest.delete()
      case "PATCH" =>
        request.body.asJson match {
          case Some(json) => wsRequest.patch(json)
          case None => wsRequest.patch("")
        }
      case _ => wsRequest.get()
    }

    futureResponse.map { response =>
      // Filter response headers as well
      val responseHeadersToExclude = Set("Content-Type", "Content-Length", "Transfer-Encoding")
      val filteredResponseHeaders = response.headers
        .filterNot { case (name, _) => responseHeadersToExclude.contains(name) }
        .map { case (k, v) => k -> v.head }
        .toSeq

      Status(response.status)(response.body)
        .withHeaders(filteredResponseHeaders*)
        .as(response.contentType)
    }
  }
}