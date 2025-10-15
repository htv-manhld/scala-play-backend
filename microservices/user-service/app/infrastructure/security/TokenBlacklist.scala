package infrastructure.security

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import scala.collection.concurrent.TrieMap

/**
 * Token Blacklist for invalidated tokens
 * Stores invalidated tokens (logout, password change, etc.)
 */
trait TokenBlacklist {
  def blacklistToken(token: String, expiresAt: LocalDateTime): Future[Unit]
  def isBlacklisted(token: String): Future[Boolean]
}

/**
 * In-memory implementation of TokenBlacklist
 * For production, consider using Redis for distributed systems
 */
@Singleton
class RedisTokenBlacklist @Inject()()(implicit ec: ExecutionContext) extends TokenBlacklist {

  // In-memory storage using concurrent map
  private val blacklist = TrieMap[String, LocalDateTime]()

  // Background task to clean up expired tokens (every 5 minutes)
  startCleanupTask()

  /**
   * Add token to blacklist
   * Store with expiration time
   */
  override def blacklistToken(token: String, expiresAt: LocalDateTime): Future[Unit] = {
    val now = LocalDateTime.now()

    // Only blacklist if token hasn't expired yet
    if (expiresAt.isAfter(now)) {
      blacklist.put(token, expiresAt)
    }

    Future.successful(())
  }

  /**
   * Check if token is blacklisted and not expired
   */
  override def isBlacklisted(token: String): Future[Boolean] = {
    val now = LocalDateTime.now()

    blacklist.get(token) match {
      case Some(expiresAt) if expiresAt.isAfter(now) =>
        Future.successful(true)
      case Some(_) =>
        // Token expired, remove from blacklist
        blacklist.remove(token)
        Future.successful(false)
      case None =>
        Future.successful(false)
    }
  }

  /**
   * Background task to clean up expired tokens
   */
  private def startCleanupTask(): Unit = {
    import scala.concurrent.duration._
    import java.util.concurrent.Executors
    import java.util.concurrent.TimeUnit

    val executor = Executors.newSingleThreadScheduledExecutor()

    executor.scheduleAtFixedRate(
      () => {
        val now = LocalDateTime.now()
        blacklist.filterInPlace { case (_, expiresAt) =>
          expiresAt.isAfter(now)
        }
      },
      5, // initial delay
      5, // period
      TimeUnit.MINUTES
    )
  }
}
