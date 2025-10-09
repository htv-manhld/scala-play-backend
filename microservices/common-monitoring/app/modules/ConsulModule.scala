package modules

import com.google.inject.AbstractModule
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Logger}
import com.orbitz.consul.Consul
import com.orbitz.consul.model.agent.{ImmutableRegistration, Registration}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton
class ConsulServiceRegistration @Inject()(
  lifecycle: ApplicationLifecycle,
  config: Configuration
) {
  private val logger = Logger(this.getClass)

  private val serviceName = config.get[String]("consul.service.name")
  private val serviceId = s"$serviceName-${java.util.UUID.randomUUID().toString}"
  private val serviceHost = config.getOptional[String]("consul.service.host").getOrElse(serviceName)
  private val servicePort = config.get[Int]("consul.service.port")
  private val httpPort = config.getOptional[Int]("play.server.http.port").getOrElse(9000)
  private val consulHost = config.getOptional[String]("consul.host").getOrElse("consul")
  private val consulPort = config.getOptional[Int]("consul.port").getOrElse(8500)

  Try {
    val consul = Consul.builder()
      .withUrl(s"http://$consulHost:$consulPort")
      .build()

    val registration: Registration = ImmutableRegistration.builder()
      .id(serviceId)
      .name(serviceName)
      .address(serviceHost)
      .port(servicePort)
      .addTags("microservice", serviceName)
      .check(Registration.RegCheck.http(s"http://$serviceHost:$httpPort/health", 10))
      .build()

    consul.agentClient().register(registration)
    logger.info(s"✅ Service registered with Consul: $serviceName at $serviceHost:$servicePort")

    // Deregister on shutdown
    lifecycle.addStopHook { () =>
      Future.successful {
        Try(consul.agentClient().deregister(serviceId)) match {
          case Success(_) => logger.info(s"✅ Service deregistered from Consul: $serviceId")
          case Failure(e) => logger.error(s"❌ Failed to deregister service from Consul: ${e.getMessage}")
        }
      }
    }
  } match {
    case Success(_) => logger.info("✅ Consul registration successful")
    case Failure(e) => logger.warn(s"⚠️ Consul registration failed: ${e.getMessage}")
  }
}

class ConsulModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[ConsulServiceRegistration]).asEagerSingleton()
  }
}
