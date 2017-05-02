---
layout: post
title:  "Apache Spark: Mapping Scala Date to Spark SQL Date"
summary: 
date: 2017-05-02
categories: Spark
tags: Spark
published: true
---  

Since the introduction of **Java 8** a new and very elegant `java.time` package (based on [Joda-Time](http://www.joda.org/joda-time/)) has been available, basically replacing the old troublesome `java.util.Date` package as well as the `java.sql.date` package.

While using the `java.time` package is very straight forward, e.g.:

```scala
import java.time._

case class SalesRecord(
  salesDate:java.time.LocalDate
  , salesAmount:Int
)

val sampleData = List(
  SalesRecord(LocalDate.parse("2017-01-01"), 20)
  , SalesRecord(LocalDate.parse("2017-01-02"),30)
  , SalesRecord(LocalDate.parse("2017-01-03"),10)
  , SalesRecord(LocalDate.parse("2017-01-04"),20)
  , SalesRecord(LocalDate.parse("2017-01-05"),10)
  , SalesRecord(LocalDate.parse("2017-01-06"),40)
  , SalesRecord(LocalDate.parse("2017-01-07"),20)
  , SalesRecord(LocalDate.parse("2017-01-08"),30)
)
``` 

Once you try to convert the sample data to a Spark dataset:

```scala
val sampleDataSet = spark.createDataset(sampleData)
```

... you get following error:

```
java.lang.UnsupportedOperationException: No Encoder found for java.time.LocalDate
```

Or if you were to create a dataframe, you would get this error:

```
java.lang.UnsupportedOperationException: Schema for type java.time.LocalDate is not supported
```

So why is this not working? To figure this out, we have to take a look at the [Spark SQL Data Type mapping](https://spark.apache.org/docs/latest/sql-programming-guide.html#data-types):

| Data type | Value type in Scala | API to access or create a data type
|----------|---------------------|------------------------------------
| `TimestampType` | `java.sql.Timestamp`   | `TimestampType`
| `DateType`      | `java.sql.Date`        | `DateType`

We can see that **Spark SQL** is using the `java.sql` package, quite likely for backward-compatibilty. So in order to make our code work, we have to change our `case class`:

```scala
import java.sql.Date

case class SalesRecord(
  salesDate:java.sql.Date
  , salesAmount:Int
)

val sampleData = List(
  SalesRecord(Date.valueOf("2017-01-01"), 20)
  , SalesRecord(Date.valueOf("2017-01-02"),30)
  , SalesRecord(Date.valueOf("2017-01-03"),10)
  , SalesRecord(Date.valueOf("2017-01-04"),20)
  , SalesRecord(Date.valueOf("2017-01-05"),10)
  , SalesRecord(Date.valueOf("2017-01-06"),40)
  , SalesRecord(Date.valueOf("2017-01-07"),20)
  , SalesRecord(Date.valueOf("2017-01-08"),30)
)

val sampleDataSet = spark.createDataset(sampleData)
```

Let's check the schema and data now:

```scala
scala> sampleDataSet.printSchema
root
 |-- salesDate: date (nullable = true)
 |-- salesAmount: integer (nullable = false)


scala> sampleDataSet.show
+----------+-----------+
| salesDate|salesAmount|
+----------+-----------+
|2017-01-01|         20|
|2017-01-02|         30|
|2017-01-03|         10|
|2017-01-04|         20|
|2017-01-05|         10|
|2017-01-06|         40|
|2017-01-07|         20|
|2017-01-08|         30|
+----------+-----------+
```

As you can see, the data types are correct now. Problem solved.