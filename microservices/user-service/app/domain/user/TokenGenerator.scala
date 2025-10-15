package domain.user

import scala.concurrent.Future
import java.time.LocalDateTime

/**
 * Domain interface for token generation
 * Implementation is provided by infrastructure layer
 */
trait TokenGenerator {
  /**
   * Generate a JWT token for a user
   * @param userId The user ID
   * @param email The user email
   * @return AuthToken containing the token value and expiration
   */
  def generateToken(userId: Long, email: String): AuthToken

  /**
   * Verify and decode a JWT token
   * @param token The token string
   * @return Option containing userId and email if valid, None if invalid/expired
   */
  def verifyToken(token: String): Option[(Long, String)]

  /**
   * Verify token and check blacklist
   * @param token The token string
   * @return Future containing Option of userId and email if valid, None if invalid/expired/blacklisted
   */
  def verifyTokenWithBlacklist(token: String): Future[Option[(Long, String)]]

  /**
   * Extract expiration time from token without full verification
   * Used for calculating blacklist TTL
   * @param token The token string
   * @return Option containing expiration time
   */
  def getExpirationTime(token: String): Option[LocalDateTime]
}
