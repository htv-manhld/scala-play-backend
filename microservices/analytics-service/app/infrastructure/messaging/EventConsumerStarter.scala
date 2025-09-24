package infrastructure.messaging

import javax.inject.{Inject, Singleton}
import play.api.inject.ApplicationLifecycle
import scala.concurrent.Future

@Singleton
class EventConsumerStarter @Inject()(
  kafkaEventSubscriber: KafkaEventSubscriber,
  domainEventHandler: DomainEventHandler,
  lifecycle: ApplicationLifecycle
) {

  kafkaEventSubscriber.subscribe(domainEventHandler)
  println("[Analytics] Kafka event consumer started - tracking all domain events...")

  lifecycle.addStopHook { () =>
    Future.successful {
      kafkaEventSubscriber.close()
      println("[Analytics] Kafka event consumer stopped")
    }
  }
}