package domain.user

import scala.concurrent.Future

/**
 * Repository interface for User aggregate
 * This is the contract that infrastructure layer must implement
 */
trait UserRepository {
  def findById(id: UserId): Future[Option[User]]
  def findByEmail(email: Email): Future[Option[User]]
  def save(user: User): Future[User]
  def delete(id: UserId): Future[Boolean]
  def findAll(): Future[Seq[User]]
  def nextId(): Future[UserId]
}

/**
 * Domain service for User operations that don't belong to a single aggregate
 */
trait UserDomainService {
  def isEmailUnique(email: Email, excludeUserId: Option[UserId] = None): Future[Boolean]
}