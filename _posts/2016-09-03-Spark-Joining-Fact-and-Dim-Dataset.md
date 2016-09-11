---
layout: post
title:  "A short exercise in Apache Spark REPL: Joining fact and dimension data"
summary: This article provides a short intro into the fascinating world of Apache Spark
date: 2016-09-04
categories: Spark
tags: Spark
published: true
--- 

This is just a very short blog post on using **Apache Spark** to create a fact table. The example is on purpose extremely simple, but it should help to understand a few key concepts.

If you haven't installed the latest version of **Apache Spark**, download it from [here](http://spark.apache.org). The installation is super easy: Basically just extract the downloaded file and save it in a convenient directory. You don't need a cluster to run it, your computer will do. Fire up **Spark REPL** (the interactive shell) like so:

```bash
bin/spark-shell
```

And you are ready to go!

> **Note**: All instructions below are for Spark version 2!

## Tabular Data: Dataset APIs

This section will explain how tabular data can be stored and manipulated in Apache Spark using three different **APIs**. Spark version 1 provided the **RDD API**, version 1.3 additionally introduced the **DataFrame API** and finally version 1.6 brought the **Dataset API**. In Spark version 2 the **DataFrame API** and **Dataset API** were merged. Note that each API provides its own methods to work with the data. The article [APACHE SPARK: RDD, DATAFRAME OR DATASET?](http://www.agildata.com/apache-spark-rdd-vs-dataframe-vs-dataset/) provides a good overview on when to use which kind of API.

### RDD API

**Definition**: Resilient Distributed Datasets (RDD) is a fundamental data structure of Spark. It is an immutable distributed collection of objects. Each dataset in RDD is divided into logical partitions, which may be computed on different nodes of the cluster. RDDs can contain any type of Python, Java, or Scala objects, including user-defined classes. Formally, an RDD is a read-only, partitioned collection of records.  [Source](http://www.tutorialspoint.com/apache_spark/apache_spark_rdd.htm)

The RDD API is a low level API.

```scala
// define schema for inline datasets
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
```

Did I mention that RDDs are the barebones, low-level dataset API in Spark? These days there are more user friendly classes available like **Datasets** and **DataFrames**.

### DataFrame API (AKA Untyped DataSet API)

**Definition**: A DataFrame is a Dataset *organized into named columns*. It is conceptually equivalent to a table in a relational database or a data frame in R/Python, but with richer optimizations under the hood. DataFrames can be constructed from a wide array of sources such as: structured data files, tables in Hive, external databases, or existing RDDs. In the **Scala API**, DataFrame is simply a type alias of `Dataset[Row]`. It also provides a **Domain Specific Language** (DSL) to project (select), filter, intersect, join, group etc. See also [here](https://jaceklaskowski.gitbooks.io/mastering-apache-spark/content/spark-sql-dataframe.html).

> "DataFrames basically do not take the data types of the column values into account. [...] In contrast to this, the new Dataset API allows modelling rows of tabular data using Scala’s case classes. [...] Datasets combine some of the benefits of Scala’s type checking with those of DataFrames. This can help to spot errors at an early stage." [Source](https://blog.codecentric.de/en/2016/07/spark-2-0-datasets-case-classes/)

We will create a DataFrame. It does not seem to make any difference if you define them either as DS or DF in our case, as we already provide the names for the columns, both `toDS` and `toDF` methods will convert correctly. As a Dataset per **definition** has no named columns we will use `toDF` here for clarity sake. Here a quick summary ([Source](https://spark.apache.org/docs/latest/sql-programming-guide.html#datasets-and-dataframes)):

```scala
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
```

### Dataset API (AKA Typed Dataset API)

**Definition**: A Dataset is a distributed collection of data. Dataset is a new interface added in Spark 1.6 that provides the benefits of RDDs (strong typing, ability to use powerful lambda functions) with the benefits of Spark SQL’s optimized execution engine. It tries to overcome some of the shortcomings of DataFrames in regard to type safety.

A **Dataset** is **type safe at compile time** whereas a DataFrames's columns will be only evaluated at run time.

> **Important**: Dataset API and DataFrame API were unified in Spark 2! [Source](https://databricks.com/blog/2016/07/14/a-tale-of-three-apache-spark-apis-rdds-dataframes-and-datasets.html). Conceptually, consider DataFrame as an alias for a collection of generic objects `Dataset[Row]`, where a `Row` is a generic **untyped** JVM object. `Dataset`, by contrast, is a collection of **strongly-typed** JVM objects, dictated by a case class you define in Scala or a class in Java.


> **Help! Dataset!? DataFrame!?**: [The Dataset API Doc](https://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.sql.Dataset) explains the difference between Datasets and DataFrame in a concisce and precise manner: "A **Dataset** is a **strongly typed** collection of domain-specific objects that can be transformed in parallel using functional or relational operations. Each Dataset also has an **untyped** view called a **DataFrame**, which is a Dataset of Row."

```scala
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
```

## Inspecting the Content and Metadata of DataFrames and Datasets

To inspect the contents and metadata of a **DataFrame** or **Dataset**, we can run these commands:

```scala
// show internal schema
scala> salesDataDF.printSchema
root
 |-- date: string (nullable = true)
 |-- officeId: integer (nullable = false)
 |-- sales: integer (nullable = false)

scala> salesDataDF.columns
res80: Array[String] = Array(date, officeId, sales)

// create SQL tables from Spark SQL Datasets - this cannot be done directly from RDD!
scala> dimOfficeDF.createOrReplaceTempView("dim_office")
scala> salesDataDF.createOrReplaceTempView("sales_data")

// show contents
scala> salesDataDF.show
+----------+--------+------+
|      date|officeId| sales|
+----------+--------+------+
|2016-01-01|     333|432245|
|2016-01-01|     234| 55432|
|2016-01-01|     231| 41123|
+----------+--------+------+

scala> dimOfficeDF.show
+--------+--------+----------+
|officeTk|officeId|officeName|
+--------+--------+----------+
|       1|     234|  New York|
|       2|     333|    London|
|       3|     231|     Milan|
+--------+--------+----------+


// show contents using SQL
scala> sql("SELECT * FROM sales_data s").show
scala> sql("SELECT * FROM dim_office o").show

// with huge datasets the following approach is recommended

scala> salesDataDF.first
res11: org.apache.spark.sql.Row = [2016-01-01,333,432245]

// or alternative you can provide a row argument with show
scala> salesDataDF.show(2)
+----------+--------+------+
|      date|officeId| sales|
+----------+--------+------+
|2016-01-01|     333|432245|
|2016-01-01|     234| 55432|
+----------+--------+------+
```

## Accessing Columns in DataFrames and Datasets

```scala
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

scala> salesDataDF.select("date").show
scala> salesDataDS.select("date").show

scala> salesDataDF.select("date", "officeId").show
scala> salesDataDS.select("date", "officeId").show
```

## Data Manipulation

It is worth checking out the API documentation to understand what's possible:

- [Dataset API Doc](https://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.sql.Dataset)
- [Dataset - Columns API Doc](https://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.sql.Column)
- [Dataset - Functions](https://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.sql.functions$) 
- [Dataset - Row](https://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.sql.Row)

> **Tip**: When looking at the particular methods etc, make sure you click on the **expand** icon on the left hand side to show an example of the usage:

![Screen Shot 2016-09-05 at 18.02.46](/images/Screen%20Shot%202016-09-05%20at%2018.02.46.png)

Next let's try to perform a simple **manipulation**: We will create the integer representation of the date, which will serve as our date technical key. There are several ways to achieve this.

First let us try to perform simple selection and creating a new field based on the value of another field:

```scala
salesDataDS.select("date", "officeId", "sales").show // works
salesDataDS.select("date", "officeId", "sales" * 2).show // does not work

// working - full dataset reference required when manipulating values
salesDataDF.select(salesDataDF("date"), salesDataDF("officeId"), salesDataDF("sales")*0.79).show
salesDataDS.select(salesDataDS("date"), salesDataDS("officeId"), salesDataDS("sales")*0.79).show

// not working: using standard Scala methods like replace on Spark SQL column - use API methods instead
salesDataDS.select(salesDataDF("date").replace("-","").toInt, salesDataDF("officeId"), salesDataDF("sales"))
// see: https://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.sql.Column
```

The `select()` **Dataset** function returns a new **Dataset**. The individual selected fields are of the Spark SQL type `Column`. For the **column** class there is a different [set of methods](https://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.sql.Column) available to perform manipulations on the data - you cannot directly apply standard **Scala** functions like `replace()`.


```scala
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
```

For more complex endeavours you can make use of the `map` function. Note the below approach of using `getAs` (a `Row` function) will **only** work with the **untyped** DataFrame. Once we retrieve the field with the `getAs` [Row function](https://spark.apache.org/docs/latest/api/scala/index.html#org.apache.spark.sql.Row) we can apply **standard Scala functions**. We use `df.getAs[String]("field")`, which means we use standard Scala String functions.

```scala
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

// create new Dataset
val salesDataTransformedDS = salesDataDF.map( s => SalesDataTransformed(
    s.getAs[String]("date").replace("-","").toInt
    , s.getAs[Int]("officeId")
    , s.getAs[Int]("sales")
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

// Trying to perform the same map transformation on the Dataset
salesDataDS.map(_.date.replace("-","").toInt).show
// same thing just slightly longer version
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
salesDataTransformedDF.createOrReplaceTempView("sales_data_transformed")
``` 

> **REPL Auto-Completion**: Especially when you start out with Spark, you will not be familiar with all the methods the API provides. The REPL (interactive shell) provides auto-completion when you hit `tab`. Awesome!

## Inspect Physical and Logic Query Plan

For the very curious person, Spark DataFrames/Datasets get processed under the hood by a query optimizer called **Catalyst**. You can see the logical and **physical query plan** as follows:

```scala
// see the logical and phsical query plan
scala> salesDataTransformedDF.explain(true)
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
```

## Joining Datasets

Next we will join the two **DataFrames** to retrieve the office **technical key**:

```scala
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
```

Note that this table will not be persisted, it only exists for your current Spark session. 

## Exporting Datasets

You could save the above table to Hive etc, for simplicity, we will just save it as a text file:

```scala
// Persisting Data
result.write.format("com.databricks.spark.csv").option("header", "true").save("/tmp/fact_sales")
```

This writes the data out partioned (so into several files). If you want to get one file one, use this approach:

```scala
result.coalesce(1).write.format("com.databricks.spark.csv").option("header", "true").save("/tmp/fact_sales_combined")
```

`coalesce(1)` combines the data in one partition.

## Summarizing Datasets

Spark offers many functions to summarize data: Here I will just quickly show two simple examples:

```scala
scala> result.groupBy("dateTk").count().show()
+--------+-----+
|  dateTk|count|
+--------+-----+
|20160101|    3|
+--------+-----+

scala> result.groupBy("dateTk").sum("sales").show()
+--------+----------+
|  dateTk|sum(sales)|
+--------+----------+
|20160101|    528800|
+--------+----------+

// not very useful with this limited dataset, but just to demonstrate how
// to use more than one column for grouping
scala> result.groupBy("dateTk", "officeTk").sum("sales").show()
```

Naturally you can do this with **SQL** as well ... but that's too easy.

## Next Steps

### Read Data From Files

Spark enables you to read files from many different formats, such as standard text files, CSV, etc but also Hadoop file formats like ORC and Parquet.

I will just quickly show you how to read a CSV file:

```scala
val df = spark.read.format("csv").option("header", "true").option("inferSchema", "true").load("file:///Users/diethardsteiner/Documents/test.csv")
```

### Read Data From a Database Table via JDBC Connection

Some of the instruction for this section are copied from [Spark: Connecting to a jdbc data-source using dataframes](http://www.infoobjects.com/spark-connecting-to-a-jdbc-data-source-using-dataframes/) and slightly adjusted.

Let us create a `person` table in **MySQL** (or a database of your choice) with following script:

```sql
USE test;
CREATE TABLE `person` (
  `person_id` int(11) NOT NULL AUTO_INCREMENT,
  `first_name` varchar(30) DEFAULT NULL,
  `last_name` varchar(30) DEFAULT NULL,
  `gender` char(1) DEFAULT NULL,
  `age` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`person_id`)
)
;
```

Now let’s insert some data to play with:

```sql
INSERT INTO PERSON 
  (first_name, last_name, gender, age)
VALUES
  ('Barack','Obama','M',53)
  , ('Bill','Clinton','M',71)
  , ('Hillary','Clinton','F',68)
  , ('Bill','Gates','M',69)
  , ('Michelle','Obama','F',51)
;
```

Download the **MySQL** jar from [here](http://dev.mysql.com/downloads/connector/j/).

Make MySQL driver available to Spark shell and launch it

```bash
$ spark-shell --driver-class-path /path-to-jdbc-jar/jdbc.jar --jars /path-to-jdbc-jar/jdbc.jar
# in example
$ spark-shell --driver-class-path /Users/diethardsteiner/Dropbox/development/jdbc-drivers/mysql-connector-java-5.1.34-bin.jar --jars /Users/diethardsteiner/Dropbox/development/jdbc-drivers/mysql-connector-java-5.1.34-bin.jar
```

#### Using Scala

Construct JDBC URL:

```scala
scala> val url="jdbc:mysql://localhost:3306/test"
```

Create connection properties object with username and password

```scala
scala> val prop = new java.util.Properties
scala> prop.setProperty("user","root")
scala> prop.setProperty("password","root")
```

Load DataFrame with JDBC data-source (url, table name, properties)

```scala
# pre Spark 2.0
scala> val people = sqlContext.read.jdbc(url,"person",prop)
# Spark 2.0 and later
scala> val people = spark.read.jdbc(url,"person",prop)
# instead of using properties you could also use map
val jdbcDF = spark.read.format("jdbc").options(
  Map("url" -> "jdbc:postgresql://localhost:5432/test",
  "dbtable" -> "schema.tablename")).load()
```

Show the results in a nice tabular format

```scala
scala> people.show
```

#### Using SQL

```scala
val person = spark.sql("CREATE TEMPORARY VIEW jdbcTable USING org.apache.spark.sql.jdbc OPTIONS ( url \"jdbc:mysql://localhost:3306/test\", user \"root\", password \"root\", dbtable \"person\")")

// or alternatively using single quotes:
val person2 = spark.sql("CREATE TEMPORARY VIEW jdbcTable2 USING org.apache.spark.sql.jdbc OPTIONS ( url 'jdbc:mysql://localhost:3306/test', user 'root', password 'root', dbtable 'person')")

// note db schema can be defined as part of the table name

scala> val sqlDF = spark.sql("SELECT * FROM jdbcTable")
```

## Conclusion

What this short exercise should demonstrate is that **Apache Spark** is fairly *easy to use*, especially since the addition of **Spark SQL**. We have barely touched the surface here in this short article, so go ahead and start exploring the Spark World!

Sources:

- [Official Spark Documentation](http://spark.apache.org/docs/latest/sql-programming-guide.html)
- [Introducing DataFrames in Apache Spark for Large Scale Data Science](https://databricks.com/blog/2015/02/17/introducing-dataframes-in-spark-for-large-scale-data-science.html)
- [Using Apache Spark DataFrames for Processing of Tabular Data](https://www.mapr.com/blog/using-apache-spark-dataframes-processing-tabular-data)
- [Spark: Connecting to a jdbc data-source using dataframes](http://www.infoobjects.com/spark-connecting-to-a-jdbc-data-source-using-dataframes/)
- [APACHE SPARK: RDD, DATAFRAME OR DATASET?](http://www.agildata.com/apache-spark-rdd-vs-dataframe-vs-dataset/)
- [Databricks Dataset Scala Notebook](https://databricks-prod-cloudfront.cloud.databricks.com/public/4027ec902e239c93eaaa8714f173bcfc/6122906529858466/431554386690871/4814681571895601/latest.html)
- [Spark 2.0 – Datasets and case classes](https://blog.codecentric.de/en/2016/07/spark-2-0-datasets-case-classes/)
- and many more


