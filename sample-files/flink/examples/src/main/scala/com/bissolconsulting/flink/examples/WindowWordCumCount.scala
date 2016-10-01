package com.bissolconsulting.flink.examples

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

/**
  * Created by dsteiner on 01/10/16.
  */
object WindowWordCumCount {
  def main(args: Array[String]) {

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val text = env.socketTextStream("localhost", 9999)

    val data:DataStream[Array[String]] = text.map { x =>
      x.split(",")
    }

    // convert Array to String and Int
    val dataTyped:DataStream[(String, Int)] = data.map { x =>
      (x(0), x(1).toInt)
    }

    //    dataTyped.print

    val counts = dataTyped
      .keyBy(0)
      .timeWindow(Time.seconds(5))
      .sum(1)

    counts.print

    env.execute("Window Stream Cumulative WordCount")

  }
}
