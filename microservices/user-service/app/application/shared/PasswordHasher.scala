package application.shared

import org.mindrot.jbcrypt.BCrypt

/**
 * Password hashing utility using BCrypt
 */
object PasswordHasher {

  /**
   * Hash a plain text password using BCrypt
   * @param plainPassword The plain text password
   * @return The hashed password
   */
  def hashPassword(plainPassword: String): String = {
    BCrypt.hashpw(plainPassword, BCrypt.gensalt(10))
  }

  /**
   * Verify a plain text password against a hashed password
   * @param plainPassword The plain text password
   * @param hashedPassword The hashed password from database
   * @return true if password matches, false otherwise
   */
  def verifyPassword(plainPassword: String, hashedPassword: String): Boolean = {
    try {
      BCrypt.checkpw(plainPassword, hashedPassword)
    } catch {
      case _: Exception => false
    }
  }
}
