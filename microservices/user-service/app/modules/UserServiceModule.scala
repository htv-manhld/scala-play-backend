package modules

import com.google.inject.AbstractModule
import domain.user.{UserRepository, UserDomainService, PasswordHasher}
import domain.user.UserDomainServiceImpl
import infrastructure.persistence.UserRepositoryImpl
import infrastructure.security.BCryptPasswordHasher
import infrastructure.messaging.{EventPublisher, KafkaEventPublisher}
import application.user.UserService
import application.shared.LoggingService

/**
 * Dependency Injection Module for User Service
 * Binds interfaces to their implementations following DDD layering
 */
class UserServiceModule extends AbstractModule {

  override def configure(): Unit = {
    // Domain Layer - Repository interfaces
    bind(classOf[UserRepository]).to(classOf[UserRepositoryImpl])

    // Domain Layer - Domain Services
    bind(classOf[UserDomainService]).to(classOf[UserDomainServiceImpl])

    // Domain Layer - Password Hasher (interface in domain, impl in infrastructure)
    bind(classOf[PasswordHasher]).to(classOf[BCryptPasswordHasher])

    // Infrastructure Layer - Event Publisher
    bind(classOf[EventPublisher]).to(classOf[KafkaEventPublisher])

    // Application Layer - Application Services
    bind(classOf[UserService])

    // Application Layer - Logging Service
    bind(classOf[LoggingService])
  }
}
