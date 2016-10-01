---
layout: post
title: "Apache Flink Streaming: Using Case Classes"
summary: This article explains how to use case classes to properly type the data sets
date: 2016-10-01
categories: Flink
tags: Flink
published: true
--- 

As in the previous article on Flink we will create a simple **tumbling window aggregation**, just this time round we will create a data model using a `case class`. In our extremely simplified example the data stream consists of purchase data from an online store.

Set the project up in a similar fashion as discussed in the previous article. We will use **IntelliJ IDEA** as our IDE.

Our ininital `Purchase` **data model** for the **parsed streaming data** looks like this:

```scala
package com.bissolconsulting.flink.examples

object Models {

  case class Purchase(
    purchaseId:Int
    , firstName:String
    , lastName:String
    , department:String
    , purchaseValue:Double
    , middleName:Option[String]
  )

}
```

In IDEA create a new pacakge called `com.bissolconsulting.flink.examples` and then a new **Scala** object file called `Models`, finally insert the code shown above.

Our program will accept an input stream from a **socket**. We parse the comma separated data and properly type it using our `Purchase` model. Finally we group the data by `department` and aggregate the `purchaseValue` (sales) - basically we want to know: 

> How much revenue do we generate in a department every 10 second?


Create a new **Scala** object file called `WindowWordCountUsingCaseClass` and insert following code:

```scala
package com.bissolconsulting.flink.examples

import com.bissolconsulting.flink.examples.Models.Purchase
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

import scala.util.Try


object WindowWordCountUsingCaseClass {
  def main(args: Array[String]) {


    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val source = env.socketTextStream("localhost", 9999)

    val parsedStream = source.map(value => {
      val columns = value.split(",")
      // the optional middleName is in the last column
      // check if it exists
      val middleName = Try(Some(columns(5))).getOrElse(None)
      // map input stream to case class model
      Purchase(columns(0).toInt, columns(1), columns(2), columns(3), columns(4).toDouble, middleName)
    })

    val keyedStream = parsedStream.keyBy(_.department)

    keyedStream
      .timeWindow(Time.seconds(10))
      .sum("purchaseValue")
      .print

    env.execute("Window Word Count Using Case Class")

  }
}
```

Next open your terminal and run this command:

```
nc -kl 9999
```

This opens a socket on port 9999.

In **IDEA** right click on our file and choose to execute it.

Next paste the below Sample Data into the terminal window and press enter:

```
111,Sam,Watson,Groceries,20
112,Luke,Waller,Groceries,50
113,Peter,Burlington,Furniture,250
114,Susan,Walker,Furniture,50,Marry
```

The output of our stream aggregation looks like this:

```
4> Purchase(111,Sam,Watson,Groceries,70.0,None)
4> Purchase(113,Peter,Burlington,Furniture,300.0,None)
```

Note that the `purchaseValue` was correctly aggregated, however, (unsurprisingly) we cannot aggregate with all the details we currently have in the records. So the next step will be to drop the unnecessary fields and introduce a new `PurchaseAggregation` **Model** using a `case class`:

```scala
package com.bissolconsulting.flink.examples

  case class Purchase(
    purchaseId:Int
    , firstName:String
    , lastName:String
    , department:String
    , purchaseValue:Double
    , middleName:Option[String]
  )

  case class PurchaseAggregation(
   department:String
   , purchaseValue:Double
 )

}
```

Finally let's add an **additional mapping** to slim the dataset down for aggreation:

```scala
package com.bissolconsulting.flink.examples

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
```

Notice that using a `case class` enables us to reference the fields by name in functions like `keyBy` and `sum`. 

Compile and **run** this version in **IDEA** and paste again the **sample data** into the terminal window. This time round the results of the **windowed aggregation** look better:

```
4> PurchaseAggregation(Groceries,70.0)
4> PurchaseAggregation(Furniture,300.0)
```

As always you can find the code for this example on my [Github repo](https://github.com/diethardsteiner/diethardsteiner.github.io/tree/master/sample-files/flink/examples).


