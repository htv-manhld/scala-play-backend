package modules

import com.google.inject.AbstractModule
import infrastructure.messaging._
import application.shared.LoggingService
import play.api.inject.ApplicationLifecycle

class AnalyticsServiceModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[LoggingService])

    // Kafka Event Subscriber - Must be eager singleton
    bind(classOf[KafkaEventSubscriber]).asEagerSingleton()
    bind(classOf[DomainEventHandler]).asEagerSingleton()
    bind(classOf[EventConsumerStarter]).asEagerSingleton()
  }
}