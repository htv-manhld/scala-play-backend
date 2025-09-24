package infrastructure.messaging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{Future, ExecutionContext}
import scala.jdk.CollectionConverters._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, ProducerConfig, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer
import play.api.libs.json.{Json, Writes}
import domain.shared.DomainEvent
import play.api.Configuration
import java.util.Properties

@Singleton
class KafkaEventPublisher @Inject()(
  config: Configuration
)(implicit ec: ExecutionContext) extends EventPublisher {

  private val kafkaBootstrapServers = config.get[String]("kafka.bootstrap.servers")
  private val kafkaTopic = config.get[String]("kafka.topic.domain-events")

  private val props = new Properties()
  props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers)
  props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
  props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
  props.put(ProducerConfig.ACKS_CONFIG, "all")
  props.put(ProducerConfig.RETRIES_CONFIG, "3")
  props.put(ProducerConfig.LINGER_MS_CONFIG, "1")

  private val producer = new KafkaProducer[String, String](props)

  override def publish(event: DomainEvent): Future[Unit] = {
    Future {
      val eventType = event.getClass.getSimpleName
      val eventJson = Json.stringify(serializeEvent(event))

      val record = new ProducerRecord[String, String](
        kafkaTopic,
        eventType,
        eventJson
      )

      try {
        val metadata = producer.send(record).get() // Wait for send to complete
        println(s"Event published: $eventType to partition ${metadata.partition()} at offset ${metadata.offset()}")
      } catch {
        case ex: Exception =>
          println(s"Error publishing event: ${ex.getMessage}")
          ex.printStackTrace()
          throw ex
      }
    }
  }

  override def publishAll(events: List[DomainEvent]): Future[Unit] = {
    Future.sequence(events.map(publish)).map(_ => ())
  }

  private def serializeEvent(event: DomainEvent): play.api.libs.json.JsValue = {
    import domain.user.events._
    import play.api.libs.json._

    event match {
      case e: UserCreated => Json.toJson(e)(UserCreated.format)
      case e: UserProfileChanged => Json.toJson(e)(UserProfileChanged.format)
      case e: UserEmailChanged => Json.toJson(e)(UserEmailChanged.format)
      case _ => Json.obj("error" -> "Unknown event type")
    }
  }

  def close(): Unit = {
    producer.close()
  }
}