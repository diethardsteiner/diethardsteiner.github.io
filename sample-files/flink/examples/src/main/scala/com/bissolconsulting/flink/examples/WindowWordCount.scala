package com.bissolconsulting.flink.examples

/**
  * Created by dsteiner on 01/10/16.
  */

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

object WindowWordCount {

    def main(args: Array[String]) {

      val env = StreamExecutionEnvironment.getExecutionEnvironment
      val text = env.socketTextStream("localhost", 9999)

      val keyValue = text.flatMap { _.toLowerCase.split("\\W+") filter { _.nonEmpty } }
        .map { (_, 1) }
        .keyBy(0)

      // examples below based on https://github.com/phatak-dev/flink-examples

      //tumbling window : Calculate wordcount for each 15 seconds
      val tumblingWindow = keyValue.timeWindow(Time.seconds(15))
      // sliding window : Calculate wordcount for last 5 seconds
      val slidingWindow = keyValue.timeWindow(Time.seconds(15),Time.seconds(5))
      //count window : Calculate for every 5 records
      val countWindow = keyValue.countWindow(5)

      tumblingWindow.sum(1).name("tumblingwindow").print()
      //slidingWindow.sum(1).name("slidingwindow").print()
      //countWindow.sum(1).name("count window").print()

      // counts.writeAsCsv("/tmp/streamSink.csv", WriteMode.OVERWRITE)
      env.execute("Window Stream WordCount")
    }
}
