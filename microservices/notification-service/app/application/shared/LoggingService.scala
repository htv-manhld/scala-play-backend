package application.shared

import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.libs.json.{Json, Writes}
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

/**
 * Centralized logging service for structured logging
 */
@Singleton
class LoggingService @Inject()()(implicit ec: ExecutionContext) extends Logging {

  case class LogEntry(
    timestamp: LocalDateTime,
    level: String,
    service: String,
    operation: String,
    message: String,
    params: Option[Map[String, String]] = None,
    userId: Option[Long] = None,
    requestId: Option[String] = None,
    duration: Option[Long] = None
  )

  implicit val logEntryWrites: Writes[LogEntry] = Json.writes[LogEntry]

  /**
   * Log structured info
   */
  def logInfo(
    operation: String,
    message: String,
    params: Map[String, String] = Map.empty,
    userId: Option[Long] = None,
    requestId: Option[String] = None
  ): Unit = {
    val entry = LogEntry(
      timestamp = LocalDateTime.now(),
      level = "INFO",
      service = "user-service",
      operation = operation,
      message = message,
      params = if (params.nonEmpty) Some(params) else None,
      userId = userId,
      requestId = requestId
    )

    logger.info(Json.stringify(Json.toJson(entry)))
  }

  /**
   * Log structured warning
   */
  def logWarning(
    operation: String,
    message: String,
    params: Map[String, String] = Map.empty,
    userId: Option[Long] = None,
    requestId: Option[String] = None
  ): Unit = {
    val entry = LogEntry(
      timestamp = LocalDateTime.now(),
      level = "WARN",
      service = "user-service",
      operation = operation,
      message = message,
      params = if (params.nonEmpty) Some(params) else None,
      userId = userId,
      requestId = requestId
    )

    logger.warn(Json.stringify(Json.toJson(entry)))
  }

  /**
   * Log structured error
   */
  def logError(
    operation: String,
    message: String,
    error: Throwable,
    params: Map[String, String] = Map.empty,
    userId: Option[Long] = None,
    requestId: Option[String] = None
  ): Unit = {
    val entry = LogEntry(
      timestamp = LocalDateTime.now(),
      level = "ERROR",
      service = "user-service",
      operation = operation,
      message = s"$message - ${error.getMessage}",
      params = if (params.nonEmpty) Some(params) else None,
      userId = userId,
      requestId = requestId
    )

    logger.error(Json.stringify(Json.toJson(entry)), error)
  }

  /**
   * Log performance metrics
   */
  def logPerformance(
    operation: String,
    duration: Long,
    params: Map[String, String] = Map.empty,
    userId: Option[Long] = None,
    requestId: Option[String] = None
  ): Unit = {
    val entry = LogEntry(
      timestamp = LocalDateTime.now(),
      level = "INFO",
      service = "user-service",
      operation = operation,
      message = s"Operation completed in ${duration}ms",
      params = if (params.nonEmpty) Some(params) else None,
      userId = userId,
      requestId = requestId,
      duration = Some(duration)
    )

    logger.info(Json.stringify(Json.toJson(entry)))
  }

  /**
   * Time and log operation execution
   */
  def timed[T](operation: String, userId: Option[Long] = None, requestId: Option[String] = None)(
    block: => Future[T]
  ): Future[T] = {
    val startTime = System.currentTimeMillis()

    logInfo(operation, "Operation started", userId = userId, requestId = requestId)

    val result = block

    result.foreach { _ =>
      val duration = System.currentTimeMillis() - startTime
      logPerformance(operation, duration, userId = userId, requestId = requestId)
    }

    result.recover { error =>
      val duration = System.currentTimeMillis() - startTime
      logError(operation, "Operation failed", error,
        params = Map("duration_ms" -> duration.toString),
        userId = userId,
        requestId = requestId)
      throw error
    }

    result
  }
}