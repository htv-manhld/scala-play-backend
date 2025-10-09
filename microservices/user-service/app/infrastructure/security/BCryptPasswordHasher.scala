package infrastructure.security

import javax.inject.Singleton
import org.mindrot.jbcrypt.BCrypt
import domain.user.PasswordHasher

/**
 * BCrypt implementation of PasswordHasher
 * This is infrastructure concern - implementation detail
 */
@Singleton
class BCryptPasswordHasher extends PasswordHasher {

  /**
   * Hash a plain text password using BCrypt
   * @param plainPassword The plain text password
   * @return The hashed password
   */
  override def hash(plainPassword: String): String = {
    BCrypt.hashpw(plainPassword, BCrypt.gensalt(10))
  }

  /**
   * Verify a plain text password against a hashed password
   * @param plainPassword The plain text password
   * @param hashedPassword The hashed password from database
   * @return true if password matches, false otherwise
   */
  override def verify(plainPassword: String, hashedPassword: String): Boolean = {
    try {
      BCrypt.checkpw(plainPassword, hashedPassword)
    } catch {
      case _: Exception => false
    }
  }
}
