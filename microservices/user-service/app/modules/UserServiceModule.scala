package modules

import com.google.inject.AbstractModule
import domain.user.{UserRepository, UserDomainService, PasswordHasher, TokenGenerator, PasswordResetTokenRepository}
import domain.user.UserDomainServiceImpl
import infrastructure.persistence.{UserRepositoryImpl, PasswordResetTokenRepositoryImpl}
import infrastructure.security.{BCryptPasswordHasher, JWTTokenGenerator, TokenBlacklist, RedisTokenBlacklist}
import infrastructure.messaging.{EventPublisher, KafkaEventPublisher, KafkaEventSubscriber}
import infrastructure.email.{EmailService, MockEmailService}
import application.user.{UserService, UserAuthService}
import application.shared.LoggingService

/**
 * Dependency Injection Module for User Service
 * Binds interfaces to their implementations following DDD layering
 */
class UserServiceModule extends AbstractModule {

  override def configure(): Unit = {
    // Domain Layer - Repository interfaces
    bind(classOf[UserRepository]).to(classOf[UserRepositoryImpl])
    bind(classOf[PasswordResetTokenRepository]).to(classOf[PasswordResetTokenRepositoryImpl])

    // Domain Layer - Domain Services
    bind(classOf[UserDomainService]).to(classOf[UserDomainServiceImpl])

    // Domain Layer - Password Hasher (interface in domain, impl in infrastructure)
    bind(classOf[PasswordHasher]).to(classOf[BCryptPasswordHasher])

    // Domain Layer - Token Generator (interface in domain, impl in infrastructure)
    bind(classOf[TokenGenerator]).to(classOf[JWTTokenGenerator])

    // Infrastructure Layer - Token Blacklist (in-memory implementation)
    bind(classOf[TokenBlacklist]).to(classOf[RedisTokenBlacklist])

    // Infrastructure Layer - Email Service (mock implementation)
    bind(classOf[EmailService]).to(classOf[MockEmailService])

    // Infrastructure Layer - Event Publisher
    bind(classOf[EventPublisher]).to(classOf[KafkaEventPublisher])

    // Infrastructure Layer - Event Subscriber
    bind(classOf[KafkaEventSubscriber]).asEagerSingleton()

    // Application Layer - Application Services
    bind(classOf[UserService])
    bind(classOf[UserAuthService])

    // Application Layer - Logging Service
    bind(classOf[LoggingService])
  }
}
