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

// Actually we should use a DataFrame instead

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

// show internal schema
salesDataDF.printSchema
root
 |-- date: string (nullable = true)
 |-- officeId: integer (nullable = false)
 |-- sales: integer (nullable = false)

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

// generating the date technical key -- testing
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

salesDataDF.map(salesData => salesData.col("date").replace("-","").toInt).show

// define new schema
case class SalesDataTransformed (
  dateTk: Int
  , officeId:Int
  , sales:Int
)

// create new DataFrame
val salesDataTransformedDF= salesDataDF.map(salesData => SalesDataTransformed(
    salesData.getAs[String]("date").replace("-","").toInt
    , salesData.getAs[Int]("officeId")
    , salesData.getAs[Int]("sales")
  )
)

scala> salesDataTransformedDF.show
+--------+--------+------+
|  dateTk|officeId| sales|
+--------+--------+------+
|20160101|     333|432245|
|20160101|     234| 55432|
|20160101|     231| 41123|
+--------+--------+------+

// alternatively use the SQL approach
sql("""
  SELECT
    CAST(TRANSLATE(date,'-','') AS INT) AS dateTk
    , officeId 
    , sales
  FROM sales_data
""").show

// register as table
salesDataTransformedDF.createOrReplaceTempView("sales_data_transformed")

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
salesDataTransformedDF.join(dimOfficeDF, dimOfficeDF("officeId") <=> salesDataTransformedDF("officeId")).show
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
val result = salesDataTransformedDF.join(dimOfficeDF, dimOfficeDF("officeId") <=> salesDataTransformedDF("officeId")).select(
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