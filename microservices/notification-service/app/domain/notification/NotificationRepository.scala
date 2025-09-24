package domain.notification

import scala.concurrent.Future
import domain.shared.DomainError

trait NotificationRepository {
  def findById(id: NotificationId): Future[Option[Notification]]
  def findByUserId(userId: UserId, page: Int = 0, size: Int = 20): Future[Seq[Notification]]
  def findAll(page: Int = 0, size: Int = 20): Future[Seq[Notification]]
  def save(notification: Notification): Future[Either[DomainError, Notification]]
  def delete(id: NotificationId): Future[Either[DomainError, Unit]]
  def nextIdentity(): Future[NotificationId]
}