package infrastructure.messaging

import scala.concurrent.Future
import domain.shared.DomainEvent

/**
 * Event publisher interface
 * Could be implemented with Kafka, RabbitMQ, etc.
 */
trait EventPublisher {
  def publish(event: DomainEvent): Future[Unit]
  def publishAll(events: List[DomainEvent]): Future[Unit]
}

/**
 * Simple in-memory event publisher for development
 * In production, this would publish to Kafka or other message broker
 */
class InMemoryEventPublisher extends EventPublisher {
  override def publish(event: DomainEvent): Future[Unit] = {
    println(s"[EVENT] Published: ${event.getClass.getSimpleName} for aggregate ${event.aggregateId.value}")
    Future.successful(())
  }

  override def publishAll(events: List[DomainEvent]): Future[Unit] = {
    events.foreach(event =>
      println(s"[EVENT] Published: ${event.getClass.getSimpleName} for aggregate ${event.aggregateId.value}")
    )
    Future.successful(())
  }
}