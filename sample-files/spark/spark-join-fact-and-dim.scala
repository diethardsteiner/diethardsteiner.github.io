// define schema
case class DimOffice (
  officeTk:Int
  , officeId:Int
  , officeName:String
)

case class SalesData (
  date:String
  , officeId:Int
  , sales:Int
)

// create RDD
val dimOfficeRDD = sc.parallelize(
  List(
    DimOffice(1, 234, "New York")
    , DimOffice(2, 333, "London")
    , DimOffice(3,231,"Milan")
  )
)

val factSalesRDD = sc.parallelize(
  List(
    SalesData("2016-01-01",333,432245)
    , SalesData("2016-01-01",234,55432)
    , SalesData("2016-01-01",231,41123)
  )
)

// Create DataFrame

val dimOfficeDF = List(
  DimOffice(1, 234, "New York")
  , DimOffice(2, 333, "London")
  , DimOffice(3,231,"Milan")
).toDF

val salesDataDF = List(
  SalesData("2016-01-01",333,432245)
  , SalesData("2016-01-01",234,55432)
  , SalesData("2016-01-01",231,41123)
).toDF


// Create Spark Dataset

val dimOfficeDS = List(
  DimOffice(1, 234, "New York")
  , DimOffice(2, 333, "London")
  , DimOffice(3,231,"Milan")
).toDS

val salesDataDS = List(
  SalesData("2016-01-01",333,432245)
  , SalesData("2016-01-01",234,55432)
  , SalesData("2016-01-01",231,41123)
).toDS



// show internal schema
salesDataDF.printSchema
root
 |-- date: string (nullable = true)
 |-- officeId: integer (nullable = false)
 |-- sales: integer (nullable = false)

salesDataDF.columns

// create SQL tables from Spark SQL Datasets - this cannot be done directly from RDD!
dimOfficeDF.createOrReplaceTempView("dim_office")
salesDataDF.createOrReplaceTempView("sales_data")

// -- INSPECTING THE CONTENT OF THE DATAFRAME

// show contents
salesDataDF.show
+----------+--------+------+
|      date|officeId| sales|
+----------+--------+------+
|2016-01-01|     333|432245|
|2016-01-01|     234| 55432|
|2016-01-01|     231| 41123|
+----------+--------+------+

dimOfficeDF.show
+--------+--------+----------+
|officeTk|officeId|officeName|
+--------+--------+----------+
|       1|     234|  New York|
|       2|     333|    London|
|       3|     231|     Milan|
+--------+--------+----------+


// show contents using SQL
sql("SELECT * FROM sales_data s").show
sql("SELECT * FROM dim_office o").show

// with huge datasets the following approach is recommended

salesDataDF.first
res11: org.apache.spark.sql.Row = [2016-01-01,333,432245]

// or alternative you can provide a row argument with show
salesDataDF.show(2)
+----------+--------+------+
|      date|officeId| sales|
+----------+--------+------+
|2016-01-01|     333|432245|
|2016-01-01|     234| 55432|
+----------+--------+------+


// ACCESSING COLUMNS

// access a specific column
// within functions use this approach
scala> salesDataDF("date")
res12: org.apache.spark.sql.Column = date

// same result as above
scala> salesDataDF.col("date")
res13: org.apache.spark.sql.Column = date

// to create a new DF from a selection of columns
scala> salesDataDF.select("date")
res14: org.apache.spark.sql.DataFrame = [date: string

salesDataDF.select("date").show
salesDataDS.select("date").show

salesDataDF.select("date", "officeId").show
salesDataDS.select("date", "officeId").show

// GENERATING THE DATE TECHNICAL KEY

salesDataDS.select("date", "officeId", "sales").show // works
salesDataDS.select("date", "officeId", "sales" * 2).show // does not work

// working - full dataset reference required when manipulating values
salesDataDF.select(salesDataDF("date"), salesDataDF("officeId"), salesDataDF("sales")*0.79).show
salesDataDS.select(salesDataDS("date"), salesDataDS("officeId"), salesDataDS("sales")*0.79).show

// not working: using standard Scala methods like replace on Spark SQL column - use API methods instead
salesDataDS.select(salesDataDF("date").replace("-","").toInt, salesDataDF("officeId"), salesDataDF("sales"))
// see: https://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.sql.Column


// works
translate(salesDataDS("date"),"-","")
// returns type org.apache.spark.sql.Column
// let's convert the result to int now as well
translate(salesDataDS("date"),"-","").cast("int")
// construct the full new dataset

// adding a new column holding a transformed value of an existing column
scala> salesDataDS.withColumn("dateTk", translate(salesDataDS("date"),"-","").cast("int")).show 
+----------+--------+------+--------+
|      date|officeId| sales|  dateTk|
+----------+--------+------+--------+
|2016-01-01|     333|432245|20160101|
|2016-01-01|     234| 55432|20160101|
|2016-01-01|     231| 41123|20160101|
+----------+--------+------+--------+

// or alternatively use this approach
scala> salesDataDS.select(salesDataDS("date"), salesDataDS("officeId"), salesDataDS("sales"), translate(salesDataDS("date"),"-","").cast("int").name("dateTk")).show
+----------+--------+------+--------+
|      date|officeId| sales|  dateTk|
+----------+--------+------+--------+
|2016-01-01|     333|432245|20160101|
|2016-01-01|     234| 55432|20160101|
|2016-01-01|     231| 41123|20160101|
+----------+--------+------+--------+

scala> salesDataDS.select(translate(salesDataDS("date"),"-","").cast("int").name("dateTk"), salesDataDS("officeId"), salesDataDS("sales")).show

+--------+--------+------+
|  dateTk|officeId| sales|
+--------+--------+------+
|20160101|     333|432245|
|20160101|     234| 55432|
|20160101|     231| 41123|
+--------+--------+------+

// for more complex endeavours you can make use of the map function
// Note the below approach of using `getAs` (a Row function) 
// will **only** work with the **untyped** DataFrame
// Once we retrieve the field with the `getAs` row function 
// we can apply standard Scala functions

salesDataDF.map(salesData => salesData
  .getAs[String]("date")
  .replace("-","")
  .toInt
).show
+--------+
|   value|
+--------+
|20160101|
|20160101|
|20160101|
+--------+

// define new schema
case class SalesDataTransformed (
  dateTk: Int
  , officeId:Int
  , sales:Int
)

// create new DataSet
val salesDataTransformedDS = salesDataDF.map( s => SalesDataTransformed(
    s.getAs[String]("date").replace("-","").toInt
    , s.getAs[Int]("officeId")
    , s.getAs[Int]("sales")
  )
)

scala> salesDataTransformedDS.show
+--------+--------+------+
|  dateTk|officeId| sales|
+--------+--------+------+
|20160101|     333|432245|
|20160101|     234| 55432|
|20160101|     231| 41123|
+--------+--------+------+

// Trying to perform the same map transformation on the Dataset
salesDataDS.map(_.date.replace("-","").toInt).show
// same thing just more verbose
salesDataDS.map(s => s.date.replace("-","").toInt).show

val salesDataTransformedDS = salesDataDS.map( s => SalesDataTransformed(
    s.date.replace("-","").toInt
    , s.officeId
    , s.sales
  )
)

// alternatively use the SQL approach
sql("""
  SELECT
    CAST(TRANSLATE(date,'-','') AS INT) AS dateTk
    , officeId 
    , sales
  FROM sales_data
""").show

// register as table
salesDataTransformedDS.createOrReplaceTempView("sales_data_transformed")

// LOGIC AND PHYSICAL QUERY PLAN

// see the logical and phsical query plan
scala> salesDataTransformedDS.explain(true)
== Parsed Logical Plan ==
'SerializeFromObject [assertnotnull(input[0, $line84.$read$$iw$$iw$SalesDataTransformed, true], top level non-flat input object).dateTk AS dateTk#83, assertnotnull(input[0, $line84.$read$$iw$$iw$SalesDataTransformed, true], top level non-flat input object).officeId AS officeId#84, assertnotnull(input[0, $line84.$read$$iw$$iw$SalesDataTransformed, true], top level non-flat input object).sales AS sales#85]
+- 'MapElements <function1>, obj#82: $line84.$read$$iw$$iw$SalesDataTransformed
   +- 'DeserializeToObject unresolveddeserializer(createexternalrow(getcolumnbyordinal(0, StringType).toString, getcolumnbyordinal(1, IntegerType), getcolumnbyordinal(2, IntegerType), StructField(date,StringType,true), StructField(officeId,IntegerType,false), StructField(sales,IntegerType,false))), obj#81: org.apache.spark.sql.Row
      +- LocalRelation [date#14, officeId#15, sales#16]

== Analyzed Logical Plan ==
dateTk: int, officeId: int, sales: int
SerializeFromObject [assertnotnull(input[0, $line84.$read$$iw$$iw$SalesDataTransformed, true], top level non-flat input object).dateTk AS dateTk#83, assertnotnull(input[0, $line84.$read$$iw$$iw$SalesDataTransformed, true], top level non-flat input object).officeId AS officeId#84, assertnotnull(input[0, $line84.$read$$iw$$iw$SalesDataTransformed, true], top level non-flat input object).sales AS sales#85]
+- MapElements <function1>, obj#82: $line84.$read$$iw$$iw$SalesDataTransformed
   +- DeserializeToObject createexternalrow(date#14.toString, officeId#15, sales#16, StructField(date,StringType,true), StructField(officeId,IntegerType,false), StructField(sales,IntegerType,false)), obj#81: org.apache.spark.sql.Row
      +- LocalRelation [date#14, officeId#15, sales#16]

== Optimized Logical Plan ==
SerializeFromObject [assertnotnull(input[0, $line84.$read$$iw$$iw$SalesDataTransformed, true], top level non-flat input object).dateTk AS dateTk#83, assertnotnull(input[0, $line84.$read$$iw$$iw$SalesDataTransformed, true], top level non-flat input object).officeId AS officeId#84, assertnotnull(input[0, $line84.$read$$iw$$iw$SalesDataTransformed, true], top level non-flat input object).sales AS sales#85]
+- MapElements <function1>, obj#82: $line84.$read$$iw$$iw$SalesDataTransformed
   +- DeserializeToObject createexternalrow(date#14.toString, officeId#15, sales#16, StructField(date,StringType,true), StructField(officeId,IntegerType,false), StructField(sales,IntegerType,false)), obj#81: org.apache.spark.sql.Row
      +- LocalRelation [date#14, officeId#15, sales#16]

== Physical Plan ==
*SerializeFromObject [assertnotnull(input[0, $line84.$read$$iw$$iw$SalesDataTransformed, true], top level non-flat input object).dateTk AS dateTk#83, assertnotnull(input[0, $line84.$read$$iw$$iw$SalesDataTransformed, true], top level non-flat input object).officeId AS officeId#84, assertnotnull(input[0, $line84.$read$$iw$$iw$SalesDataTransformed, true], top level non-flat input object).sales AS sales#85]
+- *MapElements <function1>, obj#82: $line84.$read$$iw$$iw$SalesDataTransformed
   +- *DeserializeToObject createexternalrow(date#14.toString, officeId#15, sales#16, StructField(date,StringType,true), StructField(officeId,IntegerType,false), StructField(sales,IntegerType,false)), obj#81: org.apache.spark.sql.Row
      +- LocalTableScan [date#14, officeId#15, sales#16]


// joining the two datasets to retrieve technical office key
sql("SELECT * FROM sales_data_transformed s INNER JOIN dim_office o ON s.officeId = o.officeId").show

+----------+--------+------+--------+--------+----------+
|      date|officeId| sales|officeTk|officeId|officeName|
+----------+--------+------+--------+--------+----------+
|2016-01-01|     333|432245|       2|     333|    London|
|2016-01-01|     234| 55432|       1|     234|  New York|
|2016-01-01|     231| 41123|       3|     231|     Milan|
+----------+--------+------+--------+--------+----------+

// alternatively without SQL
salesDataTransformedDS.join(dimOfficeDF, dimOfficeDF("officeId") <=> salesDataTransformedDS("officeId")).show
+----------+--------+------+--------+--------+----------+
|      date|officeId| sales|officeTk|officeId|officeName|
+----------+--------+------+--------+--------+----------+
|2016-01-01|     333|432245|       2|     333|    London|
|2016-01-01|     234| 55432|       1|     234|  New York|
|2016-01-01|     231| 41123|       3|     231|     Milan|
+----------+--------+------+--------+--------+----------+

// note that you can also pass a 3rd argument to join
// to define the join type, e.g. 'left_join'

// since we are happy with the result we will store the result in a table
// we only keep the required columns
val result = salesDataTransformedDS.join(dimOfficeDF, dimOfficeDF("officeId") <=> salesDataTransformedDS("officeId")).select(
  "dateTk", "officeTk","sales"
)


// alternatively using sql
val result = sql("""
  SELECT
    dateTk
    , officeTk
    , sales
  FROM sales_data_transformed s 
  INNER JOIN dim_office o 
    ON s.officeId = o.officeId
""")

result.show
+--------+--------+------+
|  dateTk|officeTk| sales|
+--------+--------+------+
|20160101|       2|432245|
|20160101|       1| 55432|
|20160101|       3| 41123|
+--------+--------+------+

result.createOrReplaceTempView("fact_sales")

sql("SELECT * FROM fact_sales").show
+--------+--------+------+
|  dateTk|officeTk| sales|
+--------+--------+------+
|20160101|       2|432245|
|20160101|       1| 55432|
|20160101|       3| 41123|
+--------+--------+------+

// note that this table will not be persisted
// it only exists for your current Spark session

// Persisting Data in Hive Table

result.write.format("com.databricks.spark.csv").option("header", "true").save("/tmp/fact_sales")

// saving to one file - collecting the data in one partition
result.coalesce(1).write.format("com.databricks.spark.csv").option("header", "true").save("/tmp/fact_sales_combined")

// creating a summary

result.groupBy("dateTk").count().show()
result.groupBy("dateTk").sum("sales").show()
result.groupBy("dateTk", "officeTk").sum("sales").show()
