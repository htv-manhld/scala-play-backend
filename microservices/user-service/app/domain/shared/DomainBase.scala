package domain.shared

import java.time.LocalDateTime

/**
 * Base traits for DDD building blocks
 */
trait EntityId[T] {
  def value: T
}

trait DomainEvent {
  def occurredAt: LocalDateTime
  def aggregateId: EntityId[_]
}

trait AggregateRoot[ID <: EntityId[_]] {
  def id: ID
  def version: Long

  // Immutable events list - must be implemented by concrete aggregates
  def uncommittedEvents: List[DomainEvent]

  // Return new instance with added event - immutable approach
  protected def withEvent(event: DomainEvent): this.type
  protected def withEvents(events: List[DomainEvent]): this.type
  protected def withoutEvents(): this.type
}

// Mixin trait providing default implementation
trait EventSourcedAggregate[ID <: EntityId[_]] extends AggregateRoot[ID] {
  // Subclasses must provide this field
  protected val _uncommittedEvents: List[DomainEvent] = List.empty

  override def uncommittedEvents: List[DomainEvent] = _uncommittedEvents

  // Helper methods for subclasses to use with copy()
  protected def addEvent(event: DomainEvent)(implicit ev: this.type <:< Product): this.type = {
    // This should be called like: copy(_uncommittedEvents = _uncommittedEvents :+ event).asInstanceOf[this.type]
    // But we provide a cleaner interface
    withEvent(event)
  }

  protected def addEvents(events: List[DomainEvent])(implicit ev: this.type <:< Product): this.type = {
    withEvents(events)
  }

  def markEventsAsCommitted()(implicit ev: this.type <:< Product): this.type = {
    withoutEvents()
  }
}

sealed trait DomainError {
  def message: String
}

object DomainError {
  case class ValidationError(message: String) extends DomainError
  case class NotFound(message: String) extends DomainError
  case class InvalidOperation(message: String) extends DomainError
  case class DuplicateError(message: String) extends DomainError
}