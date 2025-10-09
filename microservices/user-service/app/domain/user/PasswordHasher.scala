package domain.user

/**
 * Domain interface for password hashing
 * This is part of domain layer - interface only
 * Implementation will be in infrastructure layer
 */
trait PasswordHasher {
  def hash(plainPassword: String): String
  def verify(plainPassword: String, hashedPassword: String): Boolean
}

/**
 * Value Object for plain text password
 * Used only during user creation/password change, never persisted
 */
case class PlainPassword(value: String) {
  require(value.nonEmpty, "Password cannot be empty")
  require(value.length >= 6, "Password must be at least 6 characters")
}

/**
 * Value Object for hashed password
 * This is what gets persisted
 */
case class HashedPassword(value: String) {
  require(value.nonEmpty, "Hashed password cannot be empty")
}
