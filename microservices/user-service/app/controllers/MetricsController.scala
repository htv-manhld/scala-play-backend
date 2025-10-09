package controllers

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import play.api.mvc.*

import java.io.StringWriter
import javax.inject.*

@Singleton
class MetricsController @Inject()(
  val controllerComponents: ControllerComponents,
  registry: CollectorRegistry
) extends BaseController {

  def metrics(): Action[AnyContent] = Action {
    val writer = new StringWriter()
    TextFormat.write004(writer, registry.metricFamilySamples())

    Ok(writer.toString)
      .as(TextFormat.CONTENT_TYPE_004)
  }
}
