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

## Dataset Structures

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

Did I mention that RDDs are the barebones dataset structure in Spark? These days there are more user friendly classes available like **Datasets** and **DataFrames**.

We will create Spark SQL Datasets or DataFrame. It does not seem to make any difference if you define them either as DS or DF in our case, as we already provide the names for the columns, both `toDS` and `toDF` methods will convert correctly. As a Dataset per **definition** has no named columns we will use `toDF` here for clarity sake. Here a quick summary ([Source](https://spark.apache.org/docs/latest/sql-programming-guide.html#datasets-and-dataframes)):

- **RDD**: Resilient Distributed Datasets (RDD) is a fundamental data structure of Spark. It is an immutable distributed collection of objects. Each dataset in RDD is divided into logical partitions, which may be computed on different nodes of the cluster. RDDs can contain any type of Python, Java, or Scala objects, including user-defined classes. Formally, an RDD is a read-only, partitioned collection of records.  [Source](http://www.tutorialspoint.com/apache_spark/apache_spark_rdd.htm)
- **Datasets**: A Dataset is a distributed collection of data. Dataset is a new interface added in Spark 1.6 that provides the benefits of RDDs (strong typing, ability to use powerful lambda functions) with the benefits of Spark SQL’s optimized execution engine.
- **DataFrames**: A DataFrame is a Dataset *organized into named columns*. It is conceptually equivalent to a table in a relational database or a data frame in R/Python, but with richer optimizations under the hood. DataFrames can be constructed from a wide array of sources such as: structured data files, tables in Hive, external databases, or existing RDDs. In the **Scala API**, DataFrame is simply a type alias of `Dataset[Row]`.

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

// show internal schema
salesDataDF.printSchema
root
 |-- date: string (nullable = true)
 |-- officeId: integer (nullable = false)
 |-- sales: integer (nullable = false)
```

To inspect the contents of a DataFrame, we can do this:

```scala
// create SQL tables from Spark SQL Datasets - this cannot be done directly from RDD!
dimOfficeDF.createOrReplaceTempView("dim_office")
salesDataDF.createOrReplaceTempView("sales_data")

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
```

## Data Manipulation

Next let's try to perform a simple **manipulation** using the `map` function: We will create the integer representation of the date, which will serve as our date technical key:

```scala
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
salesDataTransformedDF.join(dimOfficeDF, dimOfficeDF("officeId") <=> salesDataTransformedDF("officeId")).show
+----------+--------+------+--------+--------+----------+
|      date|officeId| sales|officeTk|officeId|officeName|
+----------+--------+------+--------+--------+----------+
|2016-01-01|     333|432245|       2|     333|    London|
|2016-01-01|     234| 55432|       1|     234|  New York|
|2016-01-01|     231| 41123|       3|     231|     Milan|
+----------+--------+------+--------+--------+----------+

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
val df = spark.read.format("com.databricks.spark.csv").option("header", "true").option("inferSchema", "true").load("file:///Users/diethardsteiner/Documents/test.csv")
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

##### Using Scala

Construct JDBC URL:

```
scala> val url="jdbc:mysql://localhost:3306/test"
```

Create connection properties object with username and password

```
scala> val prop = new java.util.Properties
scala> prop.setProperty("user","root")
scala> prop.setProperty("password","root")
```

Load DataFrame with JDBC data-source (url, table name, properties)

```
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

```
scala> people.show
```

##### Using SQL

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
- and many more


