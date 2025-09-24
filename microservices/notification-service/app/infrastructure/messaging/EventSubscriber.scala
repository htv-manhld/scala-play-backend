package infrastructure.messaging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.jdk.DurationConverters._
import org.apache.kafka.clients.consumer.{KafkaConsumer, ConsumerConfig, ConsumerRecords}
import org.apache.kafka.common.serialization.StringDeserializer
import play.api.libs.json.{Json, JsValue}
import play.api.Configuration
import java.util.Properties
import java.time.Duration

trait EventHandler {
  def handle(eventType: String, eventData: JsValue): Future[Unit]
}

@Singleton
class KafkaEventSubscriber @Inject()(
  config: Configuration
)(implicit ec: ExecutionContext) {

  private val kafkaBootstrapServers = config.get[String]("kafka.bootstrap.servers")
  private val kafkaTopic = config.get[String]("kafka.topic.domain-events")
  private val groupId = config.get[String]("kafka.consumer.group-id")

  private val props = new Properties()
  props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers)
  props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
  props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
  props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
  props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
  props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")

  private val consumer = new KafkaConsumer[String, String](props)

  def subscribe(handler: EventHandler): Unit = {
    consumer.subscribe(List(kafkaTopic).asJava)

    Future {
      while (true) {
        val records: ConsumerRecords[String, String] = consumer.poll(Duration.ofMillis(100))
        records.asScala.foreach { record =>
          val eventType = record.key()
          val eventData = Json.parse(record.value())

          println(s"Received event: $eventType from partition ${record.partition()} at offset ${record.offset()}")
          handler.handle(eventType, eventData)
        }
      }
    }.recover {
      case ex: Exception =>
        println(s"Error in event subscriber: ${ex.getMessage}")
        ex.printStackTrace()
    }
  }

  def close(): Unit = {
    consumer.close()
  }
}