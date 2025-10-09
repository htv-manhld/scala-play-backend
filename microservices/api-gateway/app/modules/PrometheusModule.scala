package modules

import com.google.inject.AbstractModule
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.*

class PrometheusModule extends AbstractModule {
  override def configure(): Unit = {
    val registry = CollectorRegistry.defaultRegistry

    // Register JVM metrics
    new StandardExports().register(registry)
    new MemoryPoolsExports().register(registry)
    new GarbageCollectorExports().register(registry)
    new ThreadExports().register(registry)
    new ClassLoadingExports().register(registry)

    bind(classOf[CollectorRegistry]).toInstance(registry)
  }
}
