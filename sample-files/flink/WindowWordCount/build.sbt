name := "WindowWordCount"

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.apache.flink" %% "flink-streaming-scala"  % "1.1.2",
  "org.apache.flink" %% "flink-clients"          % "1.1.2"
)