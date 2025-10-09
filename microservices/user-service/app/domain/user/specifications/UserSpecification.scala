package domain.user.specifications

import domain.user.User
import domain.shared.DomainError

/**
 * Specification Pattern for User business rules
 * Encapsulates complex business logic into reusable, composable specifications
 */
trait UserSpecification {
  /**
   * Checks if the specification is satisfied by the given user
   */
  def isSatisfiedBy(user: User): Boolean

  /**
   * Returns an error if the specification is not satisfied
   */
  def check(user: User): Either[DomainError, Unit] = {
    if (isSatisfiedBy(user)) {
      Right(())
    } else {
      Left(reasonForDissatisfaction(user))
    }
  }

  /**
   * Provides the reason why the specification is not satisfied
   */
  def reasonForDissatisfaction(user: User): DomainError

  /**
   * Combines this specification with another using AND logic
   */
  def and(other: UserSpecification): UserSpecification = {
    AndSpecification(this, other)
  }

  /**
   * Combines this specification with another using OR logic
   */
  def or(other: UserSpecification): UserSpecification = {
    OrSpecification(this, other)
  }

  /**
   * Negates this specification
   */
  def not: UserSpecification = {
    NotSpecification(this)
  }
}

// Composite Specifications

case class AndSpecification(
  left: UserSpecification,
  right: UserSpecification
) extends UserSpecification {

  override def isSatisfiedBy(user: User): Boolean =
    left.isSatisfiedBy(user) && right.isSatisfiedBy(user)

  override def reasonForDissatisfaction(user: User): DomainError = {
    if (!left.isSatisfiedBy(user)) {
      left.reasonForDissatisfaction(user)
    } else {
      right.reasonForDissatisfaction(user)
    }
  }
}

case class OrSpecification(
  left: UserSpecification,
  right: UserSpecification
) extends UserSpecification {

  override def isSatisfiedBy(user: User): Boolean =
    left.isSatisfiedBy(user) || right.isSatisfiedBy(user)

  override def reasonForDissatisfaction(user: User): DomainError =
    DomainError.InvalidOperation(
      s"Neither ${left.reasonForDissatisfaction(user).message} nor ${right.reasonForDissatisfaction(user).message}"
    )
}

case class NotSpecification(
  spec: UserSpecification
) extends UserSpecification {

  override def isSatisfiedBy(user: User): Boolean =
    !spec.isSatisfiedBy(user)

  override def reasonForDissatisfaction(user: User): DomainError =
    DomainError.InvalidOperation(s"Must not satisfy: ${spec.reasonForDissatisfaction(user).message}")
}

// Concrete Specifications

/**
 * Specification: User must be active
 */
object ActiveUserSpecification extends UserSpecification {
  override def isSatisfiedBy(user: User): Boolean = user.isActive

  override def reasonForDissatisfaction(user: User): DomainError =
    DomainError.InvalidOperation("User must be active")
}

/**
 * Specification: User must be inactive
 */
object InactiveUserSpecification extends UserSpecification {
  override def isSatisfiedBy(user: User): Boolean = !user.isActive

  override def reasonForDissatisfaction(user: User): DomainError =
    DomainError.InvalidOperation("User must be inactive")
}

/**
 * Specification: User must be verified
 */
object VerifiedUserSpecification extends UserSpecification {
  override def isSatisfiedBy(user: User): Boolean = user.isVerified

  override def reasonForDissatisfaction(user: User): DomainError =
    DomainError.InvalidOperation("User must be verified")
}

/**
 * Specification: User must be unverified
 */
object UnverifiedUserSpecification extends UserSpecification {
  override def isSatisfiedBy(user: User): Boolean = !user.isVerified

  override def reasonForDissatisfaction(user: User): DomainError =
    DomainError.InvalidOperation("User must be unverified")
}

/**
 * Specification: User can be deleted
 * Business rule: Only inactive and unverified users can be deleted
 */
object CanDeleteUserSpecification extends UserSpecification {
  private val spec = InactiveUserSpecification and UnverifiedUserSpecification

  override def isSatisfiedBy(user: User): Boolean =
    spec.isSatisfiedBy(user)

  override def reasonForDissatisfaction(user: User): DomainError =
    DomainError.InvalidOperation("User can only be deleted if inactive and unverified")
}

/**
 * Specification: User can change sensitive information
 * Business rule: Only verified users can change sensitive information
 */
object CanChangeSensitiveInfoSpecification extends UserSpecification {
  private val spec = ActiveUserSpecification and VerifiedUserSpecification

  override def isSatisfiedBy(user: User): Boolean =
    spec.isSatisfiedBy(user)

  override def reasonForDissatisfaction(user: User): DomainError =
    DomainError.InvalidOperation("User must be active and verified to change sensitive information")
}

/**
 * Specification: User has password set
 */
object HasPasswordSpecification extends UserSpecification {
  override def isSatisfiedBy(user: User): Boolean = user.password.isDefined

  override def reasonForDissatisfaction(user: User): DomainError =
    DomainError.InvalidOperation("User must have a password set")
}

/**
 * Specification: User can login
 * Business rule: Active verified users with password can login
 */
object CanLoginSpecification extends UserSpecification {
  private val spec = ActiveUserSpecification and VerifiedUserSpecification and HasPasswordSpecification

  override def isSatisfiedBy(user: User): Boolean =
    spec.isSatisfiedBy(user)

  override def reasonForDissatisfaction(user: User): DomainError =
    DomainError.InvalidOperation("User must be active, verified, and have a password to login")
}
