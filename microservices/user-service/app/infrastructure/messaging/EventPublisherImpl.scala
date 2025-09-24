package infrastructure.messaging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import application.user.EventPublisher
import domain.shared.DomainEvent
import play.api.Logger

/**
 * Simple in-memory event publisher
 * In a real microservice architecture, this would use message brokers like Kafka, RabbitMQ, etc.
 */
@Singleton
class InMemoryEventPublisher @Inject()(
  eventBus: EventBusImpl
)(implicit ec: ExecutionContext) extends EventPublisher {

  private val logger = Logger(this.getClass)

  override def publish(event: DomainEvent): Future[Unit] = {
    logger.info(s"Publishing event: ${event.getClass.getSimpleName}")
    eventBus.publish(event)
  }

  override def publishAll(events: List[DomainEvent]): Future[Unit] = {
    Future.traverse(events)(publish).map(_ => ())
  }
}

/**
 * Kafka event publisher for production use
 */
@Singleton
class KafkaEventPublisher @Inject()(
  kafkaProducer: KafkaProducer
)(implicit ec: ExecutionContext) extends EventPublisher {

  private val logger = Logger(this.getClass)

  override def publish(event: DomainEvent): Future[Unit] = {
    val topic = s"user-domain-events"
    val eventJson = EventSerializer.serialize(event)

    logger.info(s"Publishing event to Kafka topic '$topic': ${event.getClass.getSimpleName}")
    kafkaProducer.send(topic, event.getClass.getSimpleName, eventJson)
  }

  override def publishAll(events: List[DomainEvent]): Future[Unit] = {
    Future.traverse(events)(publish).map(_ => ())
  }
}

// Placeholder for Kafka producer - would be implemented with actual Kafka client
trait KafkaProducer {
  def send(topic: String, key: String, value: String): Future[Unit]
}

// Placeholder for event serialization
object EventSerializer {
  def serialize(event: DomainEvent): String = {
    // In real implementation, would use JSON or Avro
    event.toString
  }
}