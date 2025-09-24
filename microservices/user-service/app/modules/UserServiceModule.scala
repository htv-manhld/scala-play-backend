package modules

import com.google.inject.AbstractModule
import domain.user.UserRepository
import infrastructure.persistence.UserRepositoryImpl
import infrastructure.messaging.{EventPublisher, KafkaEventPublisher}
import application.user.UserService
import application.shared.LoggingService

/**
 * Dependency Injection Module for User Service
 * Binds interfaces to their implementations
 */
class UserServiceModule extends AbstractModule {

  override def configure(): Unit = {
    // Bind domain repository interface to infrastructure implementation
    bind(classOf[UserRepository]).to(classOf[UserRepositoryImpl])

    // Bind event publisher - Use Kafka for Event-Driven architecture
    bind(classOf[EventPublisher]).to(classOf[KafkaEventPublisher])

    // Bind application service
    bind(classOf[UserService])

    // Bind logging service
    bind(classOf[LoggingService])
  }
}