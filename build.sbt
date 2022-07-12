ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file(".")).settings(name := "root")

lazy val zioVersion      = "2.0.0"
lazy val zioKafkaVersion = "2.0.0"
lazy val embeddedKafkaVersion = "3.2.0"

ThisBuild / libraryDependencies ++= Seq(
  "dev.zio" %% "zio"         % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion,
  "dev.zio" %% "zio-kafka"   % zioKafkaVersion,
  "io.github.embeddedkafka" %% "embedded-kafka" % embeddedKafkaVersion
)
