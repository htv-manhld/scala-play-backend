package controllers.admin

import javax.inject._
import play.api.mvc._
import play.api.libs.json._

@Singleton
class AdminController @Inject()(
  val controllerComponents: ControllerComponents
) extends BaseController {

  def dashboard() = Action { implicit request: Request[AnyContent] =>
    Ok(Json.obj(
      "message" -> "Admin Dashboard",
      "version" -> "1.0"
    ))
  }

  def getStats() = Action.async { implicit request =>
    // Mock statistics
    scala.concurrent.Future.successful(
      Ok(Json.obj(
        "totalUsers" -> 100,
        "activeUsers" -> 85,
        "totalOrders" -> 250
      ))
    )
  }
}