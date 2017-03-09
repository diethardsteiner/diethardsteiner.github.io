---
layout: post
title:  "Adventures with Apache Spark: How to clone a record"
summary: This article discusses how to clone a row
date: 2017-03-04
categories: Spark
tags: Spark
published: true
--- 

Occasionally you might have to clone a row as part of your analysis procedure. We will discuss how we can achieve this with **Apache Spark**. This article is not meant to be an introduction to Spark: I assume that you know the basics.

## Importing a CSV file

We will use this extremely simple dataset as our starting point:

```
fruit,sales
apples,2
oranges,4
grapes,3
```

Save this into a dedicated file called `test.csv` in `/tmp` and start `spark-shell` from the same directory. I usually start spark shell from this directory so that I don't have the Hive Metastore files (`metastore_db`) lying around in unwanted locations.

Apart from the clone exercise, I will also add a few other ones. With the **Spark Shell** up and running, import the **CSV** file:

```scala
scala> val test = spark.read.option("header","true").csv("test.csv")
test: org.apache.spark.sql.DataFrame = [fruit: string, sales: string]

scala> test.show
+-------+-----+
|  fruit|sales|
+-------+-----+
| apples|    2|
|oranges|    4|
| grapes|    3|
+-------+-----+

scala> test.printSchema
root
 |-- fruit: string (nullable = true)
 |-- sales: string (nullable = true)
```

## Correcting the data

Let's assume we have to adjust the sales for oranges as they were wrong:

```scala
scala> test.foreach( r => println(if(r(0).equals("oranges")){ r(1).toString.toInt + 2 } else { r(1).toString.toInt}) )
2
6
3
```

Now that the function is working, let's use map to save the results in a new val and at the same time assign a **case class**:

```scala
scala> case class FruitSales(fruit:String, sales:Int)

scala> val adjustment = test.map( r => FruitSales(r(0).toString,if(r(0).equals("oranges")){ r(1).toString.toInt + 2 } else { r(1).toString.toInt}))
adjustment: org.apache.spark.sql.Dataset[FruitSales] = [fruit: string, sales: int]

scala> adjustment.show
+-------+-----+
|  fruit|sales|
+-------+-----+
| apples|    2|
|oranges|    6|
| grapes|    3|
+-------+-----+


scala> adjustment.printSchema
root
 |-- fruit: string (nullable = true)
 |-- sales: integer (nullable = true)
```

## Cloning Records

Next let's try to **clone rows** by outputting a list and using
`flatMap` to put the clones in dedicated new rows:

```scala
scala> val adjClone = adjustment.map(r => List(r,r))
adjClone: org.apache.spark.sql.Dataset[List[FruitSales]] = [value: array<struct<fruit:string,sales:int>>]

scala> adjClone.show
+--------------------+
|               value|
+--------------------+
|[[apples,2], [app...|
|[[oranges,6], [or...|
|[[grapes,3], [gra...|
+--------------------+

scala> adjClone.flatMap( r => r).show
+-------+-----+
|  fruit|sales|
+-------+-----+
| apples|    2|
| apples|    2|
|oranges|    6|
|oranges|    6|
| grapes|    3|
| grapes|    3|
+-------+-----+
```

[Source](http://stackoverflow.com/questions/23138352/how-to-flatten-a-collection-with-spark-scala)

Next let's make this a little bit more interesting: Let's say we only want to clone if the fruits are not apples:

```scala
scala> adjustment.foreach(r => println( if(r.fruit.equals("apples")){ List(r)  } else { List(r,r) }  ))
List(FruitSales(apples,2))
List(FruitSales(oranges,6), FruitSales(oranges,6))
List(FruitSales(grapes,3), FruitSales(grapes,3))


scala> val adjAppleClone = adjustment.map(r => if(r.fruit.equals("apples")){ List(r)  } else { List(r,r) })
adjAppleClone: org.apache.spark.sql.Dataset[List[FruitSales]] = [value: array<struct<fruit:string,sales:int>>]


scala> adjAppleClone.flatMap(r => r).show
+-------+-----+
|  fruit|sales|
+-------+-----+
| apples|    2|
|oranges|    6|
|oranges|    6|
| grapes|    3|
| grapes|    3|
+-------+-----+
```

## Dynamically Cloning Records

We are facing a big challenge now: In this contrived challenge we have to create as many rows as there are sales for a given fruit.

Sources:

- [Appending an element to the end of a list in scala](http://stackoverflow.com/questions/7794063/appending-an-element-to-the-end-of-a-list-in-scala)
- [ListBuffer](http://www.scala-lang.org/api/current/scala/collection/mutable/ListBuffer.html)
- [How to create a mutable List in Scala (ListBuffer)](http://alvinalexander.com/scala/how-to-create-mutable-list-in-scala-listbuffer-cookbook)

Let's first see how to add elements to a `ListBuffer`. Since this list collection is **not immutable** we use `var` now!

```scala
scala> var tb = scala.collection.mutable.ListBuffer[String]()
tb: scala.collection.mutable.ListBuffer[String] = ListBuffer()

scala> tb += "a"
res6: scala.collection.mutable.ListBuffer[String] = ListBuffer(a)

scala> tb += "b"
res7: scala.collection.mutable.ListBuffer[String] = ListBuffer(a, b)

scala> tb += "c"
res8: scala.collection.mutable.ListBuffer[String] = ListBuffer(a, b, c)
```

Now that we are familiar with appending elements to the `ListBuffer`, let's apply this knowledge to our challenge:

```scala
val adjCloneDyn = adjustment.map(
  r => {
    // since we are appending elements dynamically
    // we need a mutable collection
    var lb = scala.collection.mutable.ListBuffer[FruitSales]()
    var a = 1
    while(a <= r.sales){ 
      lb += r
      a += 1
    }
    // convert to List
    lb.toList
    
  }
)

scala> adjCloneDyn.flatMap(r => r).show
+-------+-----+
|  fruit|sales|
+-------+-----+
| apples|    2|
| apples|    2|
|oranges|    6|
|oranges|    6|
|oranges|    6|
|oranges|    6|
|oranges|    6|
|oranges|    6|
| grapes|    3|
| grapes|    3|
| grapes|    3|
+-------+-----+
```

Challenge completed! The drawback of this solution is that we are using an mutable collection. There are a few more elegant solutions suggested by Jacek Laskowski on [StackOverflow](http://stackoverflow.com/questions/42636195/how-to-explode-by-using-a-column-value):

### Solution 1 : Best solution

Instead of using `ListBuffer` we can simply use the `fill()` method of the `Seq` collection to create a sequence with the correct number of elements dynamically. More on `Seq.fill()` [here](http://alvinalexander.com/scala/how-create-scala-list-range-fill-tabulate-constructors#create-a-scala-list-with-the-list-class-fill-method):

```scala
scala> adjustment.flatMap(r => Seq.fill(r.sales)(r.fruit)).show
+-------+
|  value|
+-------+
| apples|
| apples|
|oranges|
|oranges|
|oranges|
|oranges|
|oranges|
|oranges|
| grapes|
| grapes|
| grapes|
+-------+


scala> adjustment.flatMap(r => Seq.fill(r.sales)(r)).show
+-------+-----+
|  fruit|sales|
+-------+-----+
| apples|    2|
| apples|    2|
|oranges|    6|
|oranges|    6|
|oranges|    6|
|oranges|    6|
|oranges|    6|
|oranges|    6|
| grapes|    3|
| grapes|    3|
| grapes|    3|
+-------+-----+
```

This approach is extremely easy to use and hence wins hands down.

### Solution 2

```scala
scala> adjustment.explode("sales", "fruit") { n: Int => 0 until n }.show
warning: there was one deprecation warning; re-run with -deprecation for details
+-------+-----+-----+
|  fruit|sales|fruit|
+-------+-----+-----+
| apples|    2|    0|
| apples|    2|    1|
|oranges|    6|    0|
|oranges|    6|    1|
|oranges|    6|    2|
|oranges|    6|    3|
|oranges|    6|    4|
|oranges|    6|    5|
| grapes|    3|    0|
| grapes|    3|    1|
| grapes|    3|    2|
+-------+-----+-----+
```

Note the deprecated warning here.

### Solution 3

And finally a way to achieve the same with `withColumn`:

```scala
scala> adjustment
  .withColumn("concat", concat($"fruit", lit(",")))
  .withColumn("repeat", expr("repeat(concat, sales)"))
  .withColumn("split", split($"repeat", ","))
  .withColumn("explode", explode($"split"))
  .show

+-------+-----+--------+--------------------+--------------------+-------+
|  fruit|sales|  concat|              repeat|               split|explode|
+-------+-----+--------+--------------------+--------------------+-------+
| apples|    2| apples,|      apples,apples,|  [apples, apples, ]| apples|
| apples|    2| apples,|      apples,apples,|  [apples, apples, ]| apples|
| apples|    2| apples,|      apples,apples,|  [apples, apples, ]|       |
|oranges|    6|oranges,|oranges,oranges,o...|[oranges, oranges...|oranges|
|oranges|    6|oranges,|oranges,oranges,o...|[oranges, oranges...|oranges|
|oranges|    6|oranges,|oranges,oranges,o...|[oranges, oranges...|oranges|
|oranges|    6|oranges,|oranges,oranges,o...|[oranges, oranges...|oranges|
|oranges|    6|oranges,|oranges,oranges,o...|[oranges, oranges...|oranges|
|oranges|    6|oranges,|oranges,oranges,o...|[oranges, oranges...|oranges|
|oranges|    6|oranges,|oranges,oranges,o...|[oranges, oranges...|       |
| grapes|    3| grapes,|grapes,grapes,gra...|[grapes, grapes, ...| grapes|
| grapes|    3| grapes,|grapes,grapes,gra...|[grapes, grapes, ...| grapes|
| grapes|    3| grapes,|grapes,grapes,gra...|[grapes, grapes, ...| grapes|
| grapes|    3| grapes,|grapes,grapes,gra...|[grapes, grapes, ...|       |
+-------+-----+--------+--------------------+--------------------+-------+
```

## Other Ways to Clone Records

[This StackOverflow Question](http://stackoverflow.com/questions/40397740/replicate-spark-row-n-times) points out another way of creating clones using the `explode` function. In the following example we will clone each row 15 times:

```scala
scala> val result = adjustment.withColumn("dummy", explode(array((0 until 15).map(lit): _*)))
result: org.apache.spark.sql.DataFrame = [fruit: string, sales: int ... 1 more field]

scala> result.show(20)
+-------+-----+-----+
|  fruit|sales|dummy|
+-------+-----+-----+
| apples|    2|    0|
| apples|    2|    1|
| apples|    2|    2|
| apples|    2|    3|
| apples|    2|    4|
| apples|    2|    5|
| apples|    2|    6|
| apples|    2|    7|
| apples|    2|    8|
| apples|    2|    9|
| apples|    2|   10|
| apples|    2|   11|
| apples|    2|   12|
| apples|    2|   13|
| apples|    2|   14|
|oranges|    6|    0|
|oranges|    6|    1|
|oranges|    6|    2|
|oranges|    6|    3|
|oranges|    6|    4|
+-------+-----+-----+
only showing top 20 rows


scala> result.drop("dummy").show(20)
+-------+-----+
|  fruit|sales|
+-------+-----+
| apples|    2|
| apples|    2|
| apples|    2|
| apples|    2|
| apples|    2|
| apples|    2|
| apples|    2|
| apples|    2|
| apples|    2|
| apples|    2|
| apples|    2|
| apples|    2|
| apples|    2|
| apples|    2|
| apples|    2|
|oranges|    6|
|oranges|    6|
|oranges|    6|
|oranges|    6|
|oranges|    6|
+-------+-----+
only showing top 20 rows
```

The Spark **Row API** has a `copy()` function (see [here](https://spark.apache.org/docs/2.1.0/api/java/org/apache/spark/sql/Row.html)), however, I couldn't find any useful example for it. If you find one, please let me know ;)

```scala
scala> val tk = org.apache.spark.sql.Row(1,"test")
tk: org.apache.spark.sql.Row = [1,test]

scala> tk.copy
res25: org.apache.spark.sql.Row = [1,test]

// alternative example
scala> adjustment.foreach(r => println(r.copy()))
FruitSales(apples,2)
FruitSales(oranges,6)
FruitSales(grapes,3)
```

The `copy` function doesn't seem to provide any advantages over just referencing the record again. The source code doesn't reveal much detail for this function either (see [here](https://github.com/apache/spark/blob/master/sql/catalyst/src/main/scala/org/apache/spark/sql/Row.scala) and [here](https://github.com/apache/spark/blob/master/sql/catalyst/src/main/scala/org/apache/spark/sql/catalyst/expressions/rows.scala)).

