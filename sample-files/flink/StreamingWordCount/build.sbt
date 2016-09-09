name := "flink-demo-wordcount"

organization := "org.myfancyorg"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
   "org.apache.flink" %% "flink-streaming-scala"  % "1.1.2",
   "org.apache.flink" %% "flink-clients"          % "1.1.2",
   "com.lihaoyi"      %% "utest"                  % "0.4.3" % "test"
)

testFrameworks += new TestFramework("utest.runner.Framework")
