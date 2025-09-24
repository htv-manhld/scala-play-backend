package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import play.api.libs.json.Json

@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index(): Action[AnyContent] = Action { implicit request =>
    val response = Json.obj(
      "service" -> "api-gateway",
      "message" -> "API Gateway is running",
      "version" -> "1.0.0"
    )
    Ok(response)
  }
}