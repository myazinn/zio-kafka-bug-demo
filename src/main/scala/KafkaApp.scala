import KafkaUtils._
import zio._
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream

import java.util.UUID

object KafkaApp extends ZIOAppDefault {

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val topic = "Topic_Name"
    val client = "Client_Name"
    val group = "Group_Name"

    val n = 1_100_000

    val produceMessages =
      Ref.make(0).flatMap { counter =>
        ZStream.repeat(UUID.randomUUID()).map(id => (s"key-$id", s"msg-$id"))
          .take(n)
          .grouped(n / 10)
          .mapZIO(produceMany(topic, _))
          .tap { chunk =>
            counter.updateAndGet(_ + 1).flatMap { c =>
              Console.printLine(s"Produced ${c * chunk.size} messages")
            }
          }
          .runDrain
      }

    val consumeMessages =
      Ref.make(0).flatMap { counter =>
        Consumer
          .subscribeAnd(Subscription.topics(topic))
          .plainStream(Serde.byteArray, Serde.byteArray)
          .take(n)
          .tap { _ =>
            counter.updateAndGet(_ + 1).flatMap { c =>
              ZIO.when(c % (n / 10) == 0)(Console.printLine(s"Consumed $c messages"))
            }
          }
          .runDrain
      }

    (produceMessages *> consumeMessages).provideLayer(Kafka.embedded >>> (producer ++ consumer(client, group)))
  }

}
