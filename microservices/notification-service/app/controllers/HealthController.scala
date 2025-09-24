package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json.Json

@Singleton
class HealthController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def health(): Action[AnyContent] = Action { implicit request =>
    val healthData = Json.obj(
      "service" -> "notification-service",
      "status" -> "ok",
      "version" -> "1.0.0",
      "timestamp" -> java.time.ZonedDateTime.now()
    )
    Ok(healthData)
  }
}