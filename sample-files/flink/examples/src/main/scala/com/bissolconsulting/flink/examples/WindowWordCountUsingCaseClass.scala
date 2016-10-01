package com.bissolconsulting.flink.examples

/**
  * Created by dsteiner on 29/09/16.
  */

//case class could be implemented below as part of the object as well

import com.bissolconsulting.flink.examples.Models.{Purchase, PurchaseAggregation}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

import scala.util.Try


object WindowWordCountUsingCaseClass {
  def main(args: Array[String]) {


    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val source = env.socketTextStream("localhost", 9999)

    //session map

    val parsedStream = source.map(value => {
      val columns = value.split(",")
      // the optional middleName is in the last column
      // check if it exists
      val middleName = Try(Some(columns(5))).getOrElse(None)
      // map input stream to case class model
      Purchase(columns(0).toInt, columns(1), columns(2), columns(3), columns(4).toDouble, middleName)
    })

    val aggBase:DataStream[PurchaseAggregation] = parsedStream.map(
      record => PurchaseAggregation(record.department,record.purchaseValue)
    )

    val keyedStream = aggBase.keyBy(_.department)

    keyedStream
      .timeWindow(Time.seconds(10))
      .sum("purchaseValue")
      .print

    env.execute("Window Word Count Using Case Class")

  }
}
