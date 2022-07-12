# zio-kafka-bug-demo

## Branches
`main` with ZIO 2 implementation, `zio-1` with ZIO 1 implementation.

## How to run
You can run `KafkaApp` within your IDE (e.g. Intellij IDEA) or with sbt.\
If you want to make sure that `java.lang.OutOfMemoryError:` is thrown, you should add `-Xmx512m` VM option or rub with `sbt -mem 512 run` command.
