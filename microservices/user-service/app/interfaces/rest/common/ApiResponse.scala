package interfaces.rest.common

import play.api.libs.json._
import java.time.{LocalDateTime, ZonedDateTime}

/**
 * Standard API response wrapper for consistent API responses
 */
case class ApiResponse[T](
  success: Boolean,
  data: Option[T] = None,
  message: String = "",
  error: Option[ApiError] = None,
  timestamp: ZonedDateTime = ZonedDateTime.now()
)

case class ApiError(
  code: String,
  message: String,
  details: Option[Map[String, String]] = None
)

object ApiResponse {
  implicit def writes[T](implicit tWrites: Writes[T]): Writes[ApiResponse[T]] = Json.writes[ApiResponse[T]]
  implicit def reads[T](implicit tReads: Reads[T]): Reads[ApiResponse[T]] = Json.reads[ApiResponse[T]]

  implicit val apiErrorWrites: Writes[ApiError] = Json.writes[ApiError]
  implicit val apiErrorReads: Reads[ApiError] = Json.reads[ApiError]

  def success[T](data: T, message: String = ""): ApiResponse[T] = {
    ApiResponse(
      success = true,
      data = Some(data),
      message = message
    )
  }

  def success(message: String): ApiResponse[Nothing] = {
    ApiResponse(
      success = true,
      message = message
    )
  }

  def error(message: String, code: String, details: Option[Map[String, String]] = None): ApiResponse[Nothing] = {
    ApiResponse(
      success = false,
      message = message,
      error = Some(ApiError(code, message, details))
    )
  }
}

/**
 * Common exception handler for controllers
 */
object ControllerExceptionHandler {
  def handleDomainError(error: domain.user.DomainError): ApiResponse[Nothing] = {
    error match {
      case domain.user.DomainError.ValidationError(message) =>
        ApiResponse.error(message, "VALIDATION_ERROR")
      case domain.user.DomainError.NotFound(message) =>
        ApiResponse.error(message, "NOT_FOUND")
      case domain.user.DomainError.InvalidOperation(message) =>
        ApiResponse.error(message, "INVALID_OPERATION")
    }
  }
}