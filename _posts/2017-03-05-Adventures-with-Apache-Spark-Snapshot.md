---
layout: post
title:  "Adventures with Apache Spark: Snapshot"
summary: This article discusses how to create a snapshot table for OLAP analysis.
date: 2017-03-05
categories: Spark
tags: Spark
published: false
--- 


In this article we will discuss a very specific task: Creating a periodic? snapshot table optimised for **OLAP/Cube**-Analysis. It's cube in the classical business intelligence sense, where an MDX query is fired off and and OLAP engine processes the data. The problem is specific, because, in the multidemensional world the measure has to exist that you are asking for: Imagine we are counting how many JIRA tickets are in a specific status (e.g. "Open"). The raw data only has one record for last Friday and the following Monday - a simple count. In **SQL** it would be fairly easy to figure out how many open JIRA cases there were on Saturday. However, in the cube world, if we crossjoin the time member for Saturday with the count measure, we will get an empty tuple returned since that measure does not exist for this particular day. The same applies for Sunday. The way to solve this dilema is to create artificial records for Saturday and Sunday, basically copying Fridays record and adjusting the date details respectively. While I am not a big fan of this solution (you basically generate Big Data here), I still haven't found any other suitable alternative for it.

Previously [LINK MISSING]() I discussed how to implement this approach with Hadoop MapReduce. I've been quite curious for a while on how to implement the same with Apache Spark. This article is a follow-up on [Adventures with Apache Spark: How to clone a record](spark/2017/03/04/Adventures-with-Apache-Spark-How-to-clone-a-record.html).

Let's start our exercise: The input data looks like this:

```
"2015-01-02", 13
"2015-01-09", 13
"2015-01-01", 12
"2015-01-03", 12
"2015-01-07", 12
"2015-01-11", 12
"2015-01-03", 15
"2015-01-04", 15
"2015-01-07", 15
```


Start **Spark-Shell** and execute the following to read the source data, sort it and provide SQL access to the data via a temporary table:

```scala
val sourceData = (
  spark
    .read
    .option("header", "true")
    .option("ignoreLeadingWhiteSpace","true")
    .option("ignoreTrailingWhiteSpace","true")
    // not really necessary as default date format any ways
    .option("dateFormat","yyyy-MM-dd HH:mm:ss.S") 
    .option("inferSchema", "true")
    .csv("file:///home/dsteiner/git/code-examples/spark/cloning/source-data.txt")
    //.as[Transaction]
  )

// sort data
val sourceDataSorted = sourceData.sort("case_id","event_date")
// next we want to retrieve the next event date for a given case id
// we will do this in Spark SQL as it is easier
sourceDataSorted.createOrReplaceTempView("events")
```

The next step is to figure out how many days are between one record for a particular `case_id` to the next record. This is very easy to accomplish using a **SQL window function**:

```scala
scala> spark.sql("SELECT case_id, event_date, LEAD(event_date,1) OVER (PARTITION BY case_id ORDER BY event_date) AS next_event_date FROM events").show
+-------+--------------------+--------------------+
|case_id|          event_date|     next_event_date|
+-------+--------------------+--------------------+
|     12|2015-01-01 00:00:...|2015-01-03 00:00:...|
|     12|2015-01-03 00:00:...|2015-01-07 00:00:...|
|     12|2015-01-07 00:00:...|2015-01-11 00:00:...|
|     12|2015-01-11 00:00:...|                null|
|     13|2015-01-02 00:00:...|2015-01-09 00:00:...|
|     13|2015-01-09 00:00:...|                null|
|     15|2015-01-03 00:00:...|2015-01-04 00:00:...|
|     15|2015-01-04 00:00:...|2015-01-07 00:00:...|
|     15|2015-01-07 00:00:...|                null|
+-------+--------------------+--------------------+
```

Let's image it's 2015-01-13 today and this is all the source data we received. We want to build the snapshot up until yesterday.

```scala
scala> spark.sql("SELECT case_id, event_date, COALESCE(LEAD(event_date,1) OVER (PARTITION BY case_id ORDER BY event_date),CAST('2015-01-13 00:00:00.0' AS TIMESTAMP)) AS next_event_date FROM events").show
+-------+--------------------+--------------------+
|case_id|          event_date|     next_event_date|
+-------+--------------------+--------------------+
|     12|2015-01-01 00:00:...|2015-01-03 00:00:...|
|     12|2015-01-03 00:00:...|2015-01-07 00:00:...|
|     12|2015-01-07 00:00:...|2015-01-11 00:00:...|
|     12|2015-01-11 00:00:...|2015-01-13 00:00:...|
|     13|2015-01-02 00:00:...|2015-01-09 00:00:...|
|     13|2015-01-09 00:00:...|2015-01-13 00:00:...|
|     15|2015-01-03 00:00:...|2015-01-04 00:00:...|
|     15|2015-01-04 00:00:...|2015-01-07 00:00:...|
|     15|2015-01-07 00:00:...|2015-01-13 00:00:...|
+-------+--------------------+--------------------+
```

This looks good now. One thing left to do is to calculate the amount of days between `event_date` and `next_event_date`:

```scala
import org.apache.spark.sql.types._

val sourceDataEnriched = spark.sql("SELECT case_id, event_date, COALESCE(LEAD(event_date,1) OVER (PARTITION BY case_id ORDER BY event_date),CAST('2015-01-13 00:00:00.0' AS TIMESTAMP)) AS next_event_date FROM events").withColumn("no_of_days_gap", ((unix_timestamp(col("next_event_date")) - unix_timestamp(col("event_date")))/60/60/24).cast(IntegerType))

scala> sourceDataEnriched.show(10)
+-------+--------------------+--------------------+--------------+
|case_id|          event_date|     next_event_date|no_of_days_gap|
+-------+--------------------+--------------------+--------------+
|     12|2015-01-01 00:00:...|2015-01-03 00:00:...|             2|
|     12|2015-01-03 00:00:...|2015-01-07 00:00:...|             4|
|     12|2015-01-07 00:00:...|2015-01-11 00:00:...|             4|
|     12|2015-01-11 00:00:...|2015-01-13 00:00:...|             2|
|     13|2015-01-02 00:00:...|2015-01-09 00:00:...|             7|
|     13|2015-01-09 00:00:...|2015-01-13 00:00:...|             4|
|     15|2015-01-03 00:00:...|2015-01-04 00:00:...|             1|
|     15|2015-01-04 00:00:...|2015-01-07 00:00:...|             3|
|     15|2015-01-07 00:00:...|2015-01-13 00:00:...|             6|
+-------+--------------------+--------------------+--------------+
```

Let's create the clones:

```
scala> sourceDataEnriched.flatMap(r => Seq.fill(r.no_of_days_gap)(r)).show
<console>:28: error: value no_of_days_gap is not a member of org.apache.spark.sql.Row
       sourceDataEnriched.flatMap(r => Seq.fill(r.no_of_days_gap)(r)).show
```

The workaround is to use a **case class**:

```scala
case class SourceDataEnriched(
  case_id:Int
  , event_date:java.sql.Timestamp
  , next_event_date:java.sql.Timestamp
  , no_of_days_gap:Int
)

val sourceDataEnriched = spark.sql("SELECT case_id, event_date, COALESCE(LEAD(event_date,1) OVER (PARTITION BY case_id ORDER BY event_date),CAST('2015-01-13 00:00:00.0' AS TIMESTAMP)) AS next_event_date FROM events").withColumn("no_of_days_gap", ((unix_timestamp(col("next_event_date")) - unix_timestamp(col("event_date")))/60/60/24).cast(IntegerType)).as[SourceDataEnriched]
```

We should be able to create the clones now:

```scala
scala> val sourceDataExploded = sourceDataEnriched.flatMap(r => Seq.fill(r.no_of_days_gap)(r))
scala> sourceDataExploded.show(10)
+-------+--------------------+--------------------+--------------+
|case_id|          event_date|     next_event_date|no_of_days_gap|
+-------+--------------------+--------------------+--------------+
|     12|2015-01-01 00:00:...|2015-01-03 00:00:...|             2|
|     12|2015-01-01 00:00:...|2015-01-03 00:00:...|             2|
|     12|2015-01-03 00:00:...|2015-01-07 00:00:...|             4|
|     12|2015-01-03 00:00:...|2015-01-07 00:00:...|             4|
|     12|2015-01-03 00:00:...|2015-01-07 00:00:...|             4|
|     12|2015-01-03 00:00:...|2015-01-07 00:00:...|             4|
|     12|2015-01-07 00:00:...|2015-01-11 00:00:...|             4|
|     12|2015-01-07 00:00:...|2015-01-11 00:00:...|             4|
|     12|2015-01-07 00:00:...|2015-01-11 00:00:...|             4|
|     12|2015-01-07 00:00:...|2015-01-11 00:00:...|             4|
+-------+--------------------+--------------------+--------------+
only showing top 10 rows
```

Finally let's see how many cases there are by day:

```scala

```
