package infrastructure.security

import javax.inject.{Inject, Singleton}
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import domain.user.{TokenGenerator, AuthToken}
import java.time.{LocalDateTime, ZoneId}
import java.util.Date
import play.api.Configuration
import scala.concurrent.{ExecutionContext, Future}

/**
 * JWT implementation of TokenGenerator
 * Uses Auth0's java-jwt library
 */
@Singleton
class JWTTokenGenerator @Inject()(
  config: Configuration,
  tokenBlacklist: TokenBlacklist
)(implicit ec: ExecutionContext) extends TokenGenerator {

  // Get JWT secret from configuration, fallback to default for dev
  private val jwtSecret: String = config.getOptional[String]("jwt.secret")
    .getOrElse("your-secret-key-change-in-production")

  // Token expiration in hours
  private val expirationHours: Int = config.getOptional[Int]("jwt.expiration.hours")
    .getOrElse(24)

  private val algorithm: Algorithm = Algorithm.HMAC256(jwtSecret)

  /**
   * Generate a JWT token for a user
   */
  override def generateToken(userId: Long, email: String): AuthToken = {
    val now = LocalDateTime.now()
    val expiresAt = now.plusHours(expirationHours)

    val token = JWT.create()
      .withSubject(userId.toString)
      .withClaim("email", email)
      .withIssuedAt(Date.from(now.atZone(ZoneId.systemDefault()).toInstant))
      .withExpiresAt(Date.from(expiresAt.atZone(ZoneId.systemDefault()).toInstant))
      .sign(algorithm)

    AuthToken(token, expiresAt)
  }

  /**
   * Verify and decode a JWT token
   */
  override def verifyToken(token: String): Option[(Long, String)] = {
    try {
      val verifier = JWT.require(algorithm).build()
      val decodedJWT = verifier.verify(token)

      val userId = decodedJWT.getSubject.toLong
      val email = decodedJWT.getClaim("email").asString()

      Some((userId, email))
    } catch {
      case _: JWTVerificationException => None
      case _: NumberFormatException => None
      case _: Exception => None
    }
  }

  /**
   * Verify token and check blacklist
   */
  override def verifyTokenWithBlacklist(token: String): Future[Option[(Long, String)]] = {
    // First verify the token signature and expiration
    verifyToken(token) match {
      case Some(userData) =>
        // Then check if it's blacklisted
        tokenBlacklist.isBlacklisted(token).map { isBlacklisted =>
          if (isBlacklisted) None else Some(userData)
        }
      case None =>
        Future.successful(None)
    }
  }

  /**
   * Extract expiration time from token without full verification
   */
  override def getExpirationTime(token: String): Option[LocalDateTime] = {
    try {
      val decodedJWT = JWT.decode(token)
      val expiresAt = decodedJWT.getExpiresAt
      if (expiresAt != null) {
        Some(LocalDateTime.ofInstant(expiresAt.toInstant, ZoneId.systemDefault()))
      } else {
        None
      }
    } catch {
      case _: Exception => None
    }
  }
}
