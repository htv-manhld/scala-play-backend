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

  private var _uncommittedEvents: List[DomainEvent] = List.empty

  def addEvent(event: DomainEvent): this.type = {
    _uncommittedEvents = _uncommittedEvents :+ event
    this
  }

  def addEvents(events: List[DomainEvent]): this.type = {
    _uncommittedEvents = _uncommittedEvents ++ events
    this
  }

  def uncommittedEvents: List[DomainEvent] = _uncommittedEvents

  def markEventsAsCommitted(): this.type = {
    _uncommittedEvents = List.empty
    this
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