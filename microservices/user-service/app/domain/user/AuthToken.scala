package domain.user

import play.api.libs.json._
import java.time.LocalDateTime

case class AuthToken(
  value: String,
  expiresAt: LocalDateTime
) {
  require(value.nonEmpty, "Token value cannot be empty")
  require(expiresAt.isAfter(LocalDateTime.now()), "Token must have future expiration")

  def isExpired: Boolean = LocalDateTime.now().isAfter(expiresAt)
  def isValid: Boolean = !isExpired
}

object AuthToken {
  implicit val localDateTimeFormat: Format[LocalDateTime] = Format(
    Reads.of[String].map(LocalDateTime.parse),
    Writes.of[String].contramap(_.toString)
  )

  implicit val authTokenFormat: Format[AuthToken] = Json.format[AuthToken]
}
