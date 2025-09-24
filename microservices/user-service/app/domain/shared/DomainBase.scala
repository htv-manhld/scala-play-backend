package domain.shared

import java.time.LocalDateTime

/**
 * Base trait for all entity IDs
 */
trait EntityId[T] {
  def value: T
}

/**
 * Base trait for all domain events
 */
trait DomainEvent {
  def occurredAt: LocalDateTime
}

/**
 * Base trait for aggregate roots
 */
trait AggregateRoot[ID <: EntityId[_]] {
  def id: ID
  def version: Long

  private var domainEvents: List[DomainEvent] = List.empty

  def getUncommittedEvents: List[DomainEvent] = domainEvents

  def markEventsAsCommitted(): Unit = {
    domainEvents = List.empty
  }

  protected def addEvents(events: List[DomainEvent]): this.type = {
    domainEvents = domainEvents ++ events
    this
  }

  protected def addEvent(event: DomainEvent): this.type = {
    domainEvents = domainEvents :+ event
    this
  }
}

/**
 * Base trait for value objects
 */
trait ValueObject

/**
 * Base trait for domain services
 */
trait DomainService

/**
 * Base trait for specifications
 */
trait Specification[T] {
  def isSatisfiedBy(candidate: T): Boolean
  def and(other: Specification[T]): Specification[T] = AndSpecification(this, other)
  def or(other: Specification[T]): Specification[T] = OrSpecification(this, other)
  def not(): Specification[T] = NotSpecification(this)
}

case class AndSpecification[T](left: Specification[T], right: Specification[T]) extends Specification[T] {
  def isSatisfiedBy(candidate: T): Boolean = left.isSatisfiedBy(candidate) && right.isSatisfiedBy(candidate)
}

case class OrSpecification[T](left: Specification[T], right: Specification[T]) extends Specification[T] {
  def isSatisfiedBy(candidate: T): Boolean = left.isSatisfiedBy(candidate) || right.isSatisfiedBy(candidate)
}

case class NotSpecification[T](spec: Specification[T]) extends Specification[T] {
  def isSatisfiedBy(candidate: T): Boolean = !spec.isSatisfiedBy(candidate)
}