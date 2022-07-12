import KafkaUtils._
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.stream.ZStream

import java.util.UUID

object KafkaApp extends App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    val topic = "Topic_Name"
    val client = "Client_Name"
    val group = "Group_Name"

    val n = 1_100_000

    val produceMessages =
      Ref.make(0).flatMap { counter =>
        ZStream.repeat(UUID.randomUUID()).map(id => (s"key-$id", s"msg-$id"))
          .take(n)
          .grouped(n / 10)
          .mapM(produceMany(topic, _))
          .tap { chunk =>
            counter.updateAndGet(_ + 1).flatMap { c =>
              console.putStrLn(s"Produced ${c * chunk.size} messages")
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
              ZIO.when(c % (n / 10) == 0)(console.putStrLn(s"Consumed $c messages"))
            }
          }
          .runDrain
      }

    (produceMessages *> consumeMessages)
      .provideLayer((ZLayer.requires[Clock with Blocking with Console] >+> Kafka.embedded) >+> (producer ++ consumer(client, group)))
      .exitCode
  }

}
