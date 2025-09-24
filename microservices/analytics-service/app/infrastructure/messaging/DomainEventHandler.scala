package infrastructure.messaging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.{JsValue, Json}

@Singleton
class DomainEventHandler @Inject()()(implicit ec: ExecutionContext) extends EventHandler {

  // In-memory metrics (in production, use Redis or Database)
  private var totalUsers: Long = 0
  private var totalNotifications: Long = 0
  private var emailsSent: Long = 0

  override def handle(eventType: String, eventData: JsValue): Future[Unit] = {
    println(s"[Analytics] Handling event type: $eventType")
    println(s"[Analytics] Event data: ${Json.stringify(eventData)}")

    eventType match {
      case "UserCreated" =>
        handleUserCreated(eventData)
      case "NotificationSent" =>
        handleNotificationSent(eventData)
      case _ =>
        println(s"[Analytics] Unhandled event type: $eventType")
        Future.successful(())
    }
  }

  private def handleUserCreated(eventData: JsValue): Future[Unit] = {
    Future {
      try {
        totalUsers += 1
        val userId = (eventData \ "userId" \ "value").asOpt[Long].getOrElse(0L)
        println(s"[Analytics] UserCreated tracked - UserID: $userId, Total users: $totalUsers")
      } catch {
        case ex: Exception =>
          println(s"[Analytics] Error handling UserCreated: ${ex.getMessage}")
          ex.printStackTrace()
      }
    }
  }

  private def handleNotificationSent(eventData: JsValue): Future[Unit] = {
    Future {
      totalNotifications += 1
      val notificationId = (eventData \ "notificationId" \ "value").as[Long]
      println(s"[Analytics] NotificationSent tracked - Total notifications: $totalNotifications")
      emailsSent += 1
    }
  }

  // Public methods to get metrics
  def getMetrics(): Map[String, Long] = {
    Map(
      "totalUsers" -> totalUsers,
      "totalNotifications" -> totalNotifications,
      "emailsSent" -> emailsSent
    )
  }
}