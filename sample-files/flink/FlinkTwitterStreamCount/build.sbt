name := "FlinkTwitterStreamCount"

version := "1.0"

scalaVersion := "2.10.4"

organization := "com.bissolconsulting"

resolvers += "apache.snapshots" at "https://repository.apache.org/content/repositories/snapshots/"

val flinkVersion = "1.2-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.apache.flink" %% "flink-streaming-scala"  % flinkVersion,
  "org.apache.flink" %% "flink-clients"          % flinkVersion,
  "org.apache.flink" %% "flink-connector-twitter"          % flinkVersion,
  "com.typesafe.play" %% "play-json" % "2.4.8",
  "com.github.nscala-time" %% "nscala-time" % "2.14.0"
)
