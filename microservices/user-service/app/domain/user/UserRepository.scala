package domain.user

import scala.concurrent.Future
import domain.shared.{DomainError, PaginatedResponse}

/**
 * Domain repository interface - defines what the domain needs
 * Implementation will be in infrastructure layer
 */
trait UserRepository {
  def findById(id: UserId): Future[Option[User]]
  def findByEmail(email: Email): Future[Option[User]]
  def findAll(limit: Int = 10000): Future[Seq[User]]
  def findAllPaginated(page: Int = 0, size: Int = 20): Future[PaginatedResponse[User]]
  def save(user: User): Future[Either[DomainError, User]]
  def delete(id: UserId): Future[Either[DomainError, Unit]]
  def nextIdentity(): Future[UserId]
}