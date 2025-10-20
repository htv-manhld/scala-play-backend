package modules

import com.google.inject.AbstractModule
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.*
import scala.util.Try

class PrometheusModule extends AbstractModule {
  override def configure(): Unit = {
    val registry = CollectorRegistry.defaultRegistry

    // Register JVM metrics - use Try to avoid duplicate registration errors
    Try(new StandardExports().register(registry))
    Try(new MemoryPoolsExports().register(registry))
    Try(new GarbageCollectorExports().register(registry))
    Try(new ThreadExports().register(registry))
    Try(new ClassLoadingExports().register(registry))

    bind(classOf[CollectorRegistry]).toInstance(registry)
  }
}
