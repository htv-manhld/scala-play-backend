package domain.user

import domain.shared.EntityId
import java.time.LocalDateTime
import java.security.SecureRandom
import java.util.Base64

/**
 * Password Reset Token Entity ID
 * Following the same pattern as UserId
 */
sealed trait PasswordResetTokenId extends EntityId[Option[Long]] {
  def value: Option[Long]
  def isNew: Boolean
  def isPersisted: Boolean
}

case class NewPasswordResetTokenId() extends PasswordResetTokenId {
  override val value: Option[Long] = None
  override val isNew: Boolean = true
  override val isPersisted: Boolean = false
}

case class PersistedPasswordResetTokenId(id: Long) extends PasswordResetTokenId {
  override val value: Option[Long] = Some(id)
  override val isNew: Boolean = false
  override val isPersisted: Boolean = true
}

object PasswordResetTokenId {
  def newId(): PasswordResetTokenId = NewPasswordResetTokenId()
  def existing(id: Long): PasswordResetTokenId = PersistedPasswordResetTokenId(id)
  def fromLong(id: Long): PasswordResetTokenId = PersistedPasswordResetTokenId(id)
}

/**
 * Password Reset Token Domain Entity
 * Used for forgot password flow
 */
case class PasswordResetToken(
  id: PasswordResetTokenId,
  userId: UserId,
  token: String,
  expiresAt: LocalDateTime,
  used: Boolean,
  createdAt: LocalDateTime
) {
  /**
   * Check if token is expired
   */
  def isExpired: Boolean = LocalDateTime.now().isAfter(expiresAt)

  /**
   * Check if token is valid (not expired and not used)
   */
  def isValid: Boolean = !isExpired && !used

  /**
   * Mark token as used
   */
  def markAsUsed(): PasswordResetToken = {
    copy(used = true)
  }
}

object PasswordResetToken {
  /**
   * Generate a secure random token
   */
  def generateToken(): String = {
    val random = new SecureRandom()
    val bytes = new Array[Byte](32) // 256 bits
    random.nextBytes(bytes)
    Base64.getUrlEncoder.withoutPadding().encodeToString(bytes)
  }

  /**
   * Create a new password reset token
   * Default expiration: 1 hour
   */
  def create(
    userId: UserId,
    expirationHours: Int = 1
  ): PasswordResetToken = {
    val now = LocalDateTime.now()
    PasswordResetToken(
      id = PasswordResetTokenId.newId(),
      userId = userId,
      token = generateToken(),
      expiresAt = now.plusHours(expirationHours),
      used = false,
      createdAt = now
    )
  }
}

/**
 * Password Reset Token Repository Interface
 */
trait PasswordResetTokenRepository {
  import scala.concurrent.Future
  import domain.shared.DomainError

  /**
   * Save a password reset token
   */
  def save(resetToken: PasswordResetToken): Future[Either[DomainError, PasswordResetToken]]

  /**
   * Find reset token by token string
   */
  def findByToken(token: String): Future[Option[PasswordResetToken]]

  /**
   * Find all active (unused, not expired) tokens for a user
   */
  def findActiveTokensByUserId(userId: UserId): Future[Seq[PasswordResetToken]]

  /**
   * Mark all tokens for a user as used (invalidate them)
   */
  def invalidateAllTokensForUser(userId: UserId): Future[Unit]

  /**
   * Delete expired tokens (cleanup)
   */
  def deleteExpiredTokens(): Future[Int]
}
