package com.bissolconsulting.flink.examples

import org.apache.flink.streaming.api.scala._


/**
  * Created by dsteiner on 01/10/16.
  */
object WordCumCountFromFile {
  def main(args: Array[String]): Unit ={
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val filePath = "/home/dsteiner/git/diethardsteiner.github.io/sample-files/flink/StreamingWordCount/test.csv"
    val text = env.readTextFile(filePath)

    //    println( "------ " + text.getClass +  " ---------" )

    // explicit type definition not really required in this case

    val data:DataStream[Array[String]] = text.map { x =>
      //      println( "------ " + x.getClass +  " ---------" )
      x.split(",")
    }

    //    data.print()
    //    data.map{ x => println( x(0) + " " + x(1) ) }
    //    println( "------ " + data.getClass +  " ---------" )

    // convert Array to String and Int
    val dataTyped:DataStream[(String, Int)] = data.map { x =>
      (x(0), x(1).toInt)
    }

    //    dataTyped.print()

    val sortedData = dataTyped.keyBy(0)

    //    sortedData.print [OPEN]: Not sorted any more after switching to Map

    // create running / cumulative total
    val result = sortedData.sum(1)

    result.print

    env.execute("File Reader")
  }
}
