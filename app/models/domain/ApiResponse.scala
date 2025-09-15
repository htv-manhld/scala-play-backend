package models.domain

import play.api.libs.json.{Json, OFormat, Writes, Reads}

/**
 * Standard API response format
 */
case class ApiResponse[T](
  success: Boolean,
  data: Option[T] = None,
  message: Option[String] = None,
  error: Option[String] = None,
  timestamp: String
)

object ApiResponse {
  implicit def writes[T](implicit dataWrites: Writes[T]): Writes[ApiResponse[T]] = 
    Json.writes[ApiResponse[T]]
  
  implicit def reads[T](implicit dataReads: Reads[T]): Reads[ApiResponse[T]] = 
    Json.reads[ApiResponse[T]]
  
  implicit def format[T](implicit dataFormat: OFormat[T]): OFormat[ApiResponse[T]] = 
    Json.format[ApiResponse[T]]
  
  def success[T](data: T, message: Option[String] = None): ApiResponse[T] = 
    ApiResponse(
      success = true,
      data = Some(data),
      message = message,
      timestamp = java.time.Instant.now().toString
    )
  
  def error[T](error: String, message: Option[String] = None): ApiResponse[T] = 
    ApiResponse(
      success = false,
      error = Some(error),
      message = message,
      timestamp = java.time.Instant.now().toString
    )
}
