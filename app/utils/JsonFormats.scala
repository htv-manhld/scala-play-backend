package utils

import play.api.libs.json._
import models.domain._
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object JsonFormats {

  implicit val localDateTimeFormat: Format[LocalDateTime] = new Format[LocalDateTime] {
    def reads(json: JsValue): JsResult[LocalDateTime] = json match {
      case JsString(s) =>
        try {
          JsSuccess(LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        } catch {
          case _: Exception => JsError("Invalid date format")
        }
      case _ => JsError("String value expected")
    }

    def writes(dt: LocalDateTime): JsValue = JsString(dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
  }

  implicit val userFormat: Format[User] = Json.format[User]
  implicit val userCreateRequestFormat: Format[UserCreateRequest] = Json.format[UserCreateRequest]
  implicit val userUpdateRequestFormat: Format[UserUpdateRequest] = Json.format[UserUpdateRequest]
  // ApiResponse has its own implicit formats defined in companion object
  implicit val messageFormat: Format[Message] = Json.format[Message]
}