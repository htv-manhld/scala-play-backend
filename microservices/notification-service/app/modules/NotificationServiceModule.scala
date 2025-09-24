package modules

import com.google.inject.AbstractModule
import domain.notification.NotificationRepository
import infrastructure.persistence.NotificationRepositoryImpl
import infrastructure.messaging._
import application.notification.NotificationService
import application.shared.LoggingService
import play.api.inject.ApplicationLifecycle

class NotificationServiceModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[NotificationRepository]).to(classOf[NotificationRepositoryImpl])
    bind(classOf[EventPublisher]).to(classOf[KafkaEventPublisher])
    bind(classOf[NotificationService])
    bind(classOf[LoggingService])

    // Kafka Event Subscriber - Must be eager singleton
    bind(classOf[KafkaEventSubscriber]).asEagerSingleton()
    bind(classOf[UserEventHandler]).asEagerSingleton()
    bind(classOf[EventConsumerStarter]).asEagerSingleton()
  }
}