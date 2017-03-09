/** Our Dataset, so in an external file called test.csv
fruit,sales
apples,2
oranges,4
grapes,3
**/

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


// Various access methods

//// index based
////// untyped
scala> test.foreach( r => println(r(1)))
2
4
3

////// cast to int
scala> test.foreach( r => println(r(1).toString.toInt))
2
4
3

//// by name - with cast
scala> test.foreach( r => println(r.getAs[Int]("sales")))
2
4
3

scala> case class FruitSales(fruit:String, sales:Int) 
defined class fruitSales

// Let's assume we have to adjust the sales for oranges

scala> test.foreach( r => println(if(r(0).equals("oranges")){ r(1).toString.toInt + 2 } else { r(1).toString.toInt}) )
2
6
3

// Now that the function is working, let's use map to save the results in
// a new val and at the same time assign a case class

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

// Next let's try to clone rows by outputting a list and using
// flatMap to put the clones in new rows

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

// [Source](http://stackoverflow.com/questions/23138352/how-to-flatten-a-collection-with-spark-scala)

// Next let's make this a little bit more interesting:
// Let's say we only want to clone if the fruits are not apples

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

// Making this a bit more dynamic
// [Source](http://stackoverflow.com/questions/7794063/appending-an-element-to-the-end-of-a-list-in-scala)

// http://www.scala-lang.org/api/current/scala/collection/mutable/ListBuffer.html
// [How to create a mutable List in Scala (ListBuffer)](http://alvinalexander.com/scala/how-to-create-mutable-list-in-scala-listbuffer-cookbook)

// Let's first see how to add elements to a ListBuffer
// Since this list collection is not immutable we use `var` now!
scala> var tb = scala.collection.mutable.ListBuffer[String]()
tb: scala.collection.mutable.ListBuffer[String] = ListBuffer()

scala> tb += "a"
res6: scala.collection.mutable.ListBuffer[String] = ListBuffer(a)

scala> tb += "b"
res7: scala.collection.mutable.ListBuffer[String] = ListBuffer(a, b)

scala> tb += "c"
res8: scala.collection.mutable.ListBuffer[String] = ListBuffer(a, b, c)


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

// BETTER APPROACHES

// http://stackoverflow.com/questions/42636195/how-to-explode-by-using-a-column-value

// Solution 1 -- BEST SOLUTION
// more on `Seq.fill()` [here](http://alvinalexander.com/scala/how-create-scala-list-range-fill-tabulate-constructors#create-a-scala-list-with-the-list-class-fill-method)

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

// Solution 2

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

// Solution 3
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


// OTHER WAYS OF CLONING RECORDS

// [This StackOverflow Question](http://stackoverflow.com/questions/40397740/replicate-spark-row-n-times)

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

// Row copy function

scala> val tk = org.apache.spark.sql.Row(1,"test")
tk: org.apache.spark.sql.Row = [1,test]

scala> tk.copy
res25: org.apache.spark.sql.Row = [1,test]

// alternative example
scala> adjustment.foreach(r => println(r.copy()))
FruitSales(apples,2)
FruitSales(oranges,6)
FruitSales(grapes,3)


