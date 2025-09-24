package application.shared

import domain.shared.DomainEvent
import scala.concurrent.Future

/**
 * Base trait for application services
 */
trait ApplicationService

/**
 * Base trait for all commands
 */
trait Command

/**
 * Base trait for all queries
 */
trait Query

/**
 * Result wrapper for application operations
 */
sealed trait ApplicationResult[+T]
case class Success[T](value: T) extends ApplicationResult[T]
case class Failure[T](error: ApplicationError) extends ApplicationResult[T]

/**
 * Application layer errors
 */
sealed trait ApplicationError {
  def message: String
  def code: String
}

case class ValidationError(message: String, field: Option[String] = None) extends ApplicationError {
  val code = "VALIDATION_ERROR"
}

case class NotFoundError(message: String, entityType: String, entityId: String) extends ApplicationError {
  val code = "NOT_FOUND"
}

case class BusinessRuleViolationError(message: String, rule: String) extends ApplicationError {
  val code = "BUSINESS_RULE_VIOLATION"
}

case class UnauthorizedError(message: String) extends ApplicationError {
  val code = "UNAUTHORIZED"
}

case class ConcurrencyError(message: String) extends ApplicationError {
  val code = "CONCURRENCY_ERROR"
}

/**
 * Event handling
 */
trait EventHandler[T <: DomainEvent] {
  def handle(event: T): Future[Unit]
}

trait EventBus {
  def publish(event: DomainEvent): Future[Unit]
  def subscribe[T <: DomainEvent](handler: EventHandler[T])(implicit manifest: Manifest[T]): Unit
}