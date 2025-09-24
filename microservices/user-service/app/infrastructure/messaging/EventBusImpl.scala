package infrastructure.messaging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.mutable
import application.shared.{EventBus, EventHandler}
import domain.shared.DomainEvent
import domain.user._
import play.api.Logger

/**
 * Simple in-memory event bus implementation
 * In production, this would be replaced with proper message brokers
 */
@Singleton
class EventBusImpl @Inject()()(implicit ec: ExecutionContext) extends EventBus {

  private val logger = Logger(this.getClass)
  private val handlers = mutable.Map[Class[_], List[EventHandler[_]]]()

  override def publish(event: DomainEvent): Future[Unit] = {
    val eventClass = event.getClass
    logger.info(s"Publishing event: ${eventClass.getSimpleName}")

    val matchingHandlers = handlers.getOrElse(eventClass, List.empty)
    Future.traverse(matchingHandlers) { handler =>
      try {
        handler.asInstanceOf[EventHandler[DomainEvent]].handle(event)
      } catch {
        case ex: Exception =>
          logger.error(s"Error handling event ${eventClass.getSimpleName}", ex)
          Future.failed(ex)
      }
    }.map(_ => ())
  }

  override def subscribe[T <: DomainEvent](handler: EventHandler[T])(implicit manifest: Manifest[T]): Unit = {
    val eventClass = manifest.runtimeClass
    val currentHandlers = handlers.getOrElse(eventClass, List.empty)
    handlers(eventClass) = currentHandlers :+ handler
    logger.info(s"Subscribed handler for event: ${eventClass.getSimpleName}")
  }
}

/**
 * Example event handlers for user domain events
 */
@Singleton
class UserEventHandlers @Inject()()(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  // Handler for user creation events
  val userCreatedHandler = new EventHandler[UserCreated] {
    override def handle(event: UserCreated): Future[Unit] = {
      logger.info(s"Handling UserCreated event for user: ${event.userId.value}")
      // Example: Send welcome email, update analytics, etc.
      Future.successful(())
    }
  }

  // Handler for user profile changes
  val userProfileChangedHandler = new EventHandler[UserProfileChanged] {
    override def handle(event: UserProfileChanged): Future[Unit] = {
      logger.info(s"Handling UserProfileChanged event for user: ${event.userId.value}")
      // Example: Update search index, invalidate cache, etc.
      Future.successful(())
    }
  }

  // Handler for email changes
  val userEmailChangedHandler = new EventHandler[UserEmailChanged] {
    override def handle(event: UserEmailChanged): Future[Unit] = {
      logger.info(s"Handling UserEmailChanged event for user: ${event.userId.value}")
      // Example: Send email verification, update external systems, etc.
      Future.successful(())
    }
  }

  // Handler for user deactivation
  val userDeactivatedHandler = new EventHandler[UserDeactivated] {
    override def handle(event: UserDeactivated): Future[Unit] = {
      logger.info(s"Handling UserDeactivated event for user: ${event.userId.value}")
      // Example: Clean up user data, notify other services, etc.
      Future.successful(())
    }
  }
}

/**
 * Event handler registration
 */
@Singleton
class EventHandlerRegistry @Inject()(
  eventBus: EventBusImpl,
  userEventHandlers: UserEventHandlers
) {

  // Register all event handlers
  eventBus.subscribe(userEventHandlers.userCreatedHandler)
  eventBus.subscribe(userEventHandlers.userProfileChangedHandler)
  eventBus.subscribe(userEventHandlers.userEmailChangedHandler)
  eventBus.subscribe(userEventHandlers.userDeactivatedHandler)
}