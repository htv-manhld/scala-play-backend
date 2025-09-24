package modules

import com.google.inject.AbstractModule
import domain.user.{UserRepository, UserDomainService}
import infrastructure.persistence.{UserRepositoryImpl, UserDomainServiceImpl}
import application.user.EventPublisher
import infrastructure.messaging.{InMemoryEventPublisher, EventBusImpl, EventHandlerRegistry}

/**
 * Dependency injection module for User Service
 * Binds domain interfaces to infrastructure implementations
 */
class UserServiceModule extends AbstractModule {

  override def configure(): Unit = {
    // Domain layer bindings
    bind(classOf[UserRepository]).to(classOf[UserRepositoryImpl])
    bind(classOf[UserDomainService]).to(classOf[UserDomainServiceImpl])

    // Infrastructure layer bindings
    bind(classOf[EventPublisher]).to(classOf[InMemoryEventPublisher])

    // Event handling setup
    bind(classOf[EventBusImpl]).asEagerSingleton()
    bind(classOf[EventHandlerRegistry]).asEagerSingleton()
  }
}