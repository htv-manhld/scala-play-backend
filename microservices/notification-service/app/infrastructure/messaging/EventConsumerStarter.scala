package infrastructure.messaging

import javax.inject.{Inject, Singleton}
import play.api.inject.ApplicationLifecycle
import scala.concurrent.Future

@Singleton
class EventConsumerStarter @Inject()(
  kafkaEventSubscriber: KafkaEventSubscriber,
  userEventHandler: UserEventHandler,
  lifecycle: ApplicationLifecycle
) {

  // Start consuming events when application starts
  kafkaEventSubscriber.subscribe(userEventHandler)
  println("Kafka event consumer started - listening for domain events...")

  // Close Kafka consumer when application stops
  lifecycle.addStopHook { () =>
    Future.successful {
      kafkaEventSubscriber.close()
      println("Kafka event consumer stopped")
    }
  }
}