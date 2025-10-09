package domain.user

import scala.concurrent.{ExecutionContext, Future}
import domain.shared.DomainError
import domain.user.specifications.CanDeleteUserSpecification

/**
 * Domain Service for User aggregate
 * Contains business logic that doesn't naturally fit within a single aggregate
 * or involves multiple aggregates/repositories
 */
trait UserDomainService {
  /**
   * Ensures email uniqueness across all users
   * This is a domain invariant that requires repository access
   */
  def ensureEmailIsUnique(email: Email, excludeUserId: Option[UserId] = None): Future[Either[DomainError, Unit]]

  /**
   * Validates if a user can change their email
   * Business rule: Email must be unique and different from current
   */
  def validateEmailChange(user: User, newEmail: Email): Future[Either[DomainError, Unit]]

  /**
   * Business rule: Can a user be deleted?
   * Example: Only inactive and unverified users can be deleted
   */
  def canDeleteUser(user: User): Either[DomainError, Unit]

  /**
   * Create a new user with all necessary validations
   * This encapsulates the complete user creation business logic
   */
  def createUser(
    email: Email,
    profile: UserProfile,
    password: Option[String] = None,
    status: UserStatus = UserStatus.Active
  ): Future[Either[DomainError, User]]
}

/**
 * Implementation of UserDomainService
 */
@javax.inject.Singleton
class UserDomainServiceImpl @javax.inject.Inject()(
  userRepository: UserRepository
)(implicit ec: ExecutionContext) extends UserDomainService {

  override def ensureEmailIsUnique(
    email: Email,
    excludeUserId: Option[UserId] = None
  ): Future[Either[DomainError, Unit]] = {
    userRepository.findByEmail(email).map {
      case Some(existingUser) =>
        // If we're updating a user, allow them to keep their own email
        excludeUserId match {
          case Some(userId) if existingUser.id == userId =>
            Right(())
          case _ =>
            Left(DomainError.DuplicateError(s"User with email ${email.value} already exists"))
        }
      case None =>
        Right(())
    }
  }

  override def validateEmailChange(
    user: User,
    newEmail: Email
  ): Future[Either[DomainError, Unit]] = {
    if (newEmail == user.email) {
      Future.successful(Left(DomainError.InvalidOperation("New email is the same as current email")))
    } else {
      ensureEmailIsUnique(newEmail, Some(user.id))
    }
  }

  override def canDeleteUser(user: User): Either[DomainError, Unit] = {
    // Use Specification pattern for business rules
    CanDeleteUserSpecification.check(user)
  }

  override def createUser(
    email: Email,
    profile: UserProfile,
    password: Option[String] = None,
    status: UserStatus = UserStatus.Active
  ): Future[Either[DomainError, User]] = {
    for {
      // Check email uniqueness (domain invariant)
      uniquenessCheck <- ensureEmailIsUnique(email)
      result <- uniquenessCheck match {
        case Right(_) =>
          // Create the user using the factory method
          User.create(email, profile, password, status) match {
            case Right(user) => Future.successful(Right(user))
            case Left(error) => Future.successful(Left(error))
          }
        case Left(error) =>
          Future.successful(Left(error))
      }
    } yield result
  }
}
