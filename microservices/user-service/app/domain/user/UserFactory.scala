package domain.user

import java.time.LocalDateTime
import domain.shared.DomainError
import domain.user.events.UserCreated

/**
 * Factory for creating User aggregates
 * Encapsulates all validation and business rules for user creation
 * This is a Domain Service pattern specifically for object creation
 */
object UserFactory {

  /**
   * Create a new User with all validations
   * This method ensures all invariants are satisfied before creating a User
   */
  def createUser(
    email: Email,
    profile: UserProfile,
    password: Option[String] = None,
    status: UserStatus = UserStatus.Active
  ): Either[DomainError, User] = {
    for {
      // Validate email format (already done in Email value object constructor)
      // Validate profile (already done in UserProfile value object constructor)

      // Additional business validations
      _ <- validatePassword(password)
      _ <- validateUserStatus(status)

      // Create the user if all validations pass
      user <- buildUser(email, profile, password, status)
    } yield user
  }

  /**
   * Reconstruct a User from persistence (without domain events)
   * Used by Repository when loading from database
   */
  def reconstitute(
    id: UserId,
    email: Email,
    password: Option[String],
    profile: UserProfile,
    status: UserStatus,
    lastLoginAt: Option[LocalDateTime],
    verifiedAt: Option[LocalDateTime],
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    version: Long
  ): User = {
    User(
      id = id,
      email = email,
      password = password,
      profile = profile,
      status = status,
      lastLoginAt = lastLoginAt,
      verifiedAt = verifiedAt,
      createdAt = createdAt,
      updatedAt = updatedAt,
      version = version,
      _uncommittedEvents = List.empty // No events when reconstituting from DB
    )
  }

  // Private validation methods

  private def validatePassword(password: Option[String]): Either[DomainError, Unit] = {
    password match {
      case Some(pwd) if pwd.trim.isEmpty =>
        Left(DomainError.ValidationError("Password cannot be empty"))
      case Some(pwd) if pwd.length < 6 =>
        Left(DomainError.ValidationError("Password must be at least 6 characters long"))
      case _ =>
        Right(())
    }
  }

  private def validateUserStatus(status: UserStatus): Either[DomainError, Unit] = {
    // Business rule: New users can only be created as Active or Inactive
    status match {
      case UserStatus.Active | UserStatus.Inactive =>
        Right(())
      case _ =>
        Left(DomainError.ValidationError(s"Invalid initial status: ${status}"))
    }
  }

  private def buildUser(
    email: Email,
    profile: UserProfile,
    password: Option[String],
    status: UserStatus
  ): Either[DomainError, User] = {
    try {
      val now = LocalDateTime.now()
      val userId = UserId.newUser()
      val event = UserCreated(userId, email, profile, now)

      val user = User(
        id = userId,
        email = email,
        password = password,
        profile = profile,
        status = status,
        lastLoginAt = None,
        verifiedAt = None,
        createdAt = now,
        updatedAt = now,
        version = 0,
        _uncommittedEvents = List(event)
      )

      Right(user)
    } catch {
      case ex: IllegalArgumentException =>
        Left(DomainError.ValidationError(ex.getMessage))
      case ex: Exception =>
        Left(DomainError.ValidationError(s"Failed to create user: ${ex.getMessage}"))
    }
  }
}
