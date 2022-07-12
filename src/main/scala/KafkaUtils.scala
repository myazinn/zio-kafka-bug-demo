import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}
import zio._
import zio.kafka.consumer.Consumer.OffsetRetrieval
import zio.kafka.consumer._
import zio.kafka.consumer.diagnostics.Diagnostics
import zio.kafka.producer._
import zio.kafka.serde.Serde

object KafkaUtils {

  val producerSettings: ZIO[Kafka, Nothing, ProducerSettings] =
    ZIO.serviceWith[Kafka](_.bootstrapServers).map(ProducerSettings(_))

  val producer: ZLayer[Kafka, Throwable, Producer] =
    ZLayer.fromZIO(producerSettings) >>> Producer.live

  def produceMany(topic: String, kvs: Iterable[(String, String)]): ZIO[Producer, Throwable, Chunk[RecordMetadata]] =
    Producer
      .produceChunk[Any, String, String](
        Chunk.fromIterable(kvs.map { case (k, v) =>
          new ProducerRecord(topic, k, v)
        }),
        Serde.string,
        Serde.string
      )

  def consumerSettings(clientId: String, groupId: String): URIO[Kafka, ConsumerSettings] =
    ZIO.serviceWith[Kafka] { (kafka: Kafka) =>
      ConsumerSettings(kafka.bootstrapServers)
        .withClientId(clientId)
        .withCloseTimeout(5.seconds)
        .withProperties(
          ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "earliest",
          ConsumerConfig.METADATA_MAX_AGE_CONFIG -> "100",
          ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG -> "3000",
          ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG -> "250",
          ConsumerConfig.MAX_POLL_RECORDS_CONFIG -> "10",
          ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG -> "true"
        )
        .withPerPartitionChunkPrefetch(16)
        .withOffsetRetrieval(OffsetRetrieval.Auto())
        .withRestartStreamOnRebalancing(false)
        .withGroupId(groupId)
    }

  def consumer(clientId: String, groupId: String): ZLayer[Kafka, Throwable, Consumer] =
    (ZLayer(consumerSettings(clientId, groupId)) ++ ZLayer.succeed(Diagnostics.NoOp)) >>> Consumer.live

}
