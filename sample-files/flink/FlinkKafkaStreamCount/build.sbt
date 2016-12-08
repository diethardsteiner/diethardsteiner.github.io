name := "FlinkKafkaStreamCount"

version := "1.0"

scalaVersion := "2.11.8"

organization := "com.bissolconsulting"

resolvers += "apache.snapshots" at "https://repository.apache.org/content/repositories/snapshots/"

val flinkVersion = "1.2-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.apache.kafka" %% "kafka" % "0.10.1.0",
  "org.apache.flink" %% "flink-streaming-scala"  % flinkVersion,
  "org.apache.flink" %% "flink-clients"          % flinkVersion,
  "org.apache.flink" %% "flink-connector-kafka-0.10"         % flinkVersion
)
