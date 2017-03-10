
// import source data
// using outer bracket only to make it run in spark-shell
val sourceData = (
  spark
    .read
    .option("header", "true")
    .option("inferSchema", "true")
    .csv("file:///home/dsteiner/git/code-examples/spark/cloning/source-data.txt")
  )
  

// check data
scala> sourceData.columns
res36: Array[String] = Array(event_date, " case_id")
// Notice the space in the second column name!

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


sourceData.printSchema
sourceData.show
sourceData.take(10)

// SORT DATA - by caseId key
sourceData.sort("case_id","event_date").show
val sourceDataSorted = sourceData.sort("case_id","event_date")

// next we want to retrieve the next event date for a given case id
// we will do this in Spark SQL as it is easier
sourceDataSorted.createOrReplaceTempView("events")

// just making sure our table is ready
spark.sql("SELECT * FROM events").show(10)

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


// Let's image it's 2015-01-13 today and this is all the source data
// we received. We want to build the snapshot up until yesterday.

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


// This looks good now. One thing left to do is to calculate
// the amount of days between event_date and next_event_date

scala> spark.sql("SELECT case_id, event_date, COALESCE(LEAD(event_date,1) OVER (PARTITION BY case_id ORDER BY event_date),CAST('2015-01-13 00:00:00.0' AS TIMESTAMP)) AS next_event_date FROM events").withColumn("no_of_days_gap", (unix_timestamp(col("next_event_date")) - unix_timestamp(col("event_date")))/60/60/24).show

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



// Let's create the clones:

scala> sourceDataEnriched.flatMap(r => Seq.fill(r.no_of_days_gap)(r)).show
<console>:28: error: value no_of_days_gap is not a member of org.apache.spark.sql.Row
       sourceDataEnriched.flatMap(r => Seq.fill(r.no_of_days_gap)(r)).show

// -> Error, requires case class

case class SourceDataEnriched(
  case_id:Int
  , event_date:java.sql.Timestamp
  , next_event_date:java.sql.Timestamp
  , no_of_days_gap:Int
)

val sourceDataEnriched = spark.sql("SELECT case_id, event_date, COALESCE(LEAD(event_date,1) OVER (PARTITION BY case_id ORDER BY event_date),CAST('2015-01-13 00:00:00.0' AS TIMESTAMP)) AS next_event_date FROM events").withColumn("no_of_days_gap", ((unix_timestamp(col("next_event_date")) - unix_timestamp(col("event_date")))/60/60/24).cast(IntegerType)).as[SourceDataEnriched]

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

// Let's create the clone_no so we can create the correct snapshot date later on:

scala> sourceDataExploded.createOrReplaceTempView("events_ranked")

scala> val eventsWithCloneNo = spark.sql("SELECT case_id, event_date, no_of_days_gap, ROW_NUMBER() OVER (PARTITION BY case_id, event_date ORDER BY event_date) - 1 as clone_no FROM events_ranked")

scala> eventsWithCloneNo.sort("case_id","event_date","clone_no").show(50)
+-------+--------------------+--------------+--------+
|case_id|          event_date|no_of_days_gap|clone_no|
+-------+--------------------+--------------+--------+
|     12|2015-01-01 00:00:...|             2|       0|
|     12|2015-01-01 00:00:...|             2|       1|
|     12|2015-01-03 00:00:...|             4|       0|
|     12|2015-01-03 00:00:...|             4|       1|
|     12|2015-01-03 00:00:...|             4|       2|
|     12|2015-01-03 00:00:...|             4|       3|
|     12|2015-01-07 00:00:...|             4|       0|
|     12|2015-01-07 00:00:...|             4|       1|
|     12|2015-01-07 00:00:...|             4|       2|
|     12|2015-01-07 00:00:...|             4|       3|
|     12|2015-01-11 00:00:...|             2|       0|
|     12|2015-01-11 00:00:...|             2|       1|
|     13|2015-01-02 00:00:...|             7|       0|
|     13|2015-01-02 00:00:...|             7|       1|
|     13|2015-01-02 00:00:...|             7|       2|
|     13|2015-01-02 00:00:...|             7|       3|
|     13|2015-01-02 00:00:...|             7|       4|
|     13|2015-01-02 00:00:...|             7|       5|
|     13|2015-01-02 00:00:...|             7|       6|
|     13|2015-01-09 00:00:...|             4|       0|
|     13|2015-01-09 00:00:...|             4|       1|
|     13|2015-01-09 00:00:...|             4|       2|
|     13|2015-01-09 00:00:...|             4|       3|
|     15|2015-01-03 00:00:...|             1|       0|
|     15|2015-01-04 00:00:...|             3|       0|
|     15|2015-01-04 00:00:...|             3|       1|
|     15|2015-01-04 00:00:...|             3|       2|
|     15|2015-01-07 00:00:...|             6|       0|
|     15|2015-01-07 00:00:...|             6|       1|
|     15|2015-01-07 00:00:...|             6|       2|
|     15|2015-01-07 00:00:...|             6|       3|
|     15|2015-01-07 00:00:...|             6|       4|
|     15|2015-01-07 00:00:...|             6|       5|
+-------+--------------------+--------------+--------+



// Let's create the correct snapshot date
val snapshot = eventsWithCloneNo.selectExpr("date_add(event_date,clone_no) AS snapshot_date","case_id")

scala> snapshot.sort("case_id","snapshot_date").show(50)

+-------------+-------+
|snapshot_date|case_id|
+-------------+-------+
|   2015-01-01|     12|
|   2015-01-02|     12|
|   2015-01-03|     12|
|   2015-01-04|     12|
|   2015-01-05|     12|
|   2015-01-06|     12|
|   2015-01-07|     12|
|   2015-01-08|     12|
|   2015-01-09|     12|
|   2015-01-10|     12|
|   2015-01-11|     12|
|   2015-01-12|     12|
|   2015-01-02|     13|
|   2015-01-03|     13|
|   2015-01-04|     13|
|   2015-01-05|     13|
|   2015-01-06|     13|
|   2015-01-07|     13|
|   2015-01-08|     13|
|   2015-01-09|     13|
|   2015-01-10|     13|
|   2015-01-11|     13|
|   2015-01-12|     13|
|   2015-01-03|     15|
|   2015-01-04|     15|
|   2015-01-05|     15|
|   2015-01-06|     15|
|   2015-01-07|     15|
|   2015-01-08|     15|
|   2015-01-09|     15|
|   2015-01-10|     15|
|   2015-01-11|     15|
|   2015-01-12|     15|
+-------------+-------+


// Finally let's see how many cases there are by day

scala> snapshot.createOrReplaceTempView("snapshot_jira")
scala> spark.sql("SELECT snapshot_date, COUNT(*) AS cnt FROM snapshot_jira GROUP BY 1 ORDER BY 1").show

+-------------+---+
|snapshot_date|cnt|
+-------------+---+
|   2015-01-01|  1|
|   2015-01-02|  2|
|   2015-01-03|  3|
|   2015-01-04|  3|
|   2015-01-05|  3|
|   2015-01-06|  3|
|   2015-01-07|  3|
|   2015-01-08|  3|
|   2015-01-09|  3|
|   2015-01-10|  3|
|   2015-01-11|  3|
|   2015-01-12|  3|
+-------------+---+



// ALTERNATIVE SOLUTION
// not recommended due to use of ListBuffer

import java.sql.Timestamp

case class SDEnriched(
  case_id:Int
  , event_date:java.sql.Timestamp
  , next_event_date:java.sql.Timestamp
  , no_of_days_gap:Int
  , clone_no:Int
)

val sdCloneDyn = sourceDataEnriched.map(
  // row/record
  r => {
    // since we are appending elements dynamically
    // we need a mutable collection
    var lb = scala.collection.mutable.ListBuffer[SDEnriched]()
    var a = 0
    while(
      a < r.getAs[Int]("no_of_days_gap")
      //a < r.no_of_days_gap
    ){ 
      lb += new SDEnriched(
        r.getAs[Int]("case_id")
        , r.getAs[Timestamp]("event_date")
        , r.getAs[Timestamp]("next_event_date")
        , r.getAs[Int]("no_of_days_gap")
        , a
      )
      a += 1
    }
    // convert to List
    lb.toList   
  }
)

val sdCloned = sdCloneDyn.flatMap(r => r)
scala> sdCloned.show(20)

+-------+--------------------+--------------------+--------------+--------+
|case_id|          event_date|     next_event_date|no_of_days_gap|clone_no|
+-------+--------------------+--------------------+--------------+--------+
|     12|2015-01-01 00:00:...|2015-01-03 00:00:...|             2|       0|
|     12|2015-01-01 00:00:...|2015-01-03 00:00:...|             2|       1|
|     12|2015-01-03 00:00:...|2015-01-07 00:00:...|             4|       0|
|     12|2015-01-03 00:00:...|2015-01-07 00:00:...|             4|       1|
|     12|2015-01-03 00:00:...|2015-01-07 00:00:...|             4|       2|
|     12|2015-01-03 00:00:...|2015-01-07 00:00:...|             4|       3|
|     12|2015-01-07 00:00:...|2015-01-11 00:00:...|             4|       0|
|     12|2015-01-07 00:00:...|2015-01-11 00:00:...|             4|       1|
|     12|2015-01-07 00:00:...|2015-01-11 00:00:...|             4|       2|
|     12|2015-01-07 00:00:...|2015-01-11 00:00:...|             4|       3|
|     12|2015-01-11 00:00:...|2015-01-12 00:00:...|             1|       0|
|     13|2015-01-02 00:00:...|2015-01-09 00:00:...|             7|       0|
|     13|2015-01-02 00:00:...|2015-01-09 00:00:...|             7|       1|
|     13|2015-01-02 00:00:...|2015-01-09 00:00:...|             7|       2|
|     13|2015-01-02 00:00:...|2015-01-09 00:00:...|             7|       3|
|     13|2015-01-02 00:00:...|2015-01-09 00:00:...|             7|       4|
|     13|2015-01-02 00:00:...|2015-01-09 00:00:...|             7|       5|
|     13|2015-01-02 00:00:...|2015-01-09 00:00:...|             7|       6|
|     13|2015-01-09 00:00:...|2015-01-12 00:00:...|             3|       0|
|     13|2015-01-09 00:00:...|2015-01-12 00:00:...|             3|       1|
+-------+--------------------+--------------------+--------------+--------+
only showing top 20 rows

sdCloned.createOrReplaceTempView("events_cloned")

spark.sql("SELECT *,  FROM events_cloned").show

// couldn't get the case working! 
sdCloned.withColumn("snapshot_date", date_add(col("event_date"),col("clone_no").cast(IntegerType)))
// `date_add` expects a Col Type as the first argument and
// an Int Type as the second one. There doesn't seem to be a way
// to unwrap the column value.

// workaround 1
sdCloned.withColumn("snapshot_date", expr("date_add(event_date, clone_no)"))

// workaround 2
sdCloned.selectExpr("event_date","clone_no","date_add(event_date,clone_no) AS snapshot_date").show
+--------------------+--------+-------------+
|          event_date|clone_no|snapshot_date|
+--------------------+--------+-------------+
|2015-01-01 00:00:...|       0|   2015-01-01|
|2015-01-01 00:00:...|       1|   2015-01-02|
|2015-01-03 00:00:...|       0|   2015-01-03|
|2015-01-03 00:00:...|       1|   2015-01-04|
|2015-01-03 00:00:...|       2|   2015-01-05|
|2015-01-03 00:00:...|       3|   2015-01-06|
|2015-01-07 00:00:...|       0|   2015-01-07|
|2015-01-07 00:00:...|       1|   2015-01-08|
|2015-01-07 00:00:...|       2|   2015-01-09|
|2015-01-07 00:00:...|       3|   2015-01-10|
|2015-01-11 00:00:...|       0|   2015-01-11|
|2015-01-02 00:00:...|       0|   2015-01-02|
|2015-01-02 00:00:...|       1|   2015-01-03|
|2015-01-02 00:00:...|       2|   2015-01-04|
|2015-01-02 00:00:...|       3|   2015-01-05|
|2015-01-02 00:00:...|       4|   2015-01-06|
|2015-01-02 00:00:...|       5|   2015-01-07|
|2015-01-02 00:00:...|       6|   2015-01-08|
|2015-01-09 00:00:...|       0|   2015-01-09|
|2015-01-09 00:00:...|       1|   2015-01-10|
+--------------------+--------+-------------+

val snapshot = sdCloned.selectExpr("date_add(event_date,clone_no) AS snapshot_date", "case_id")
snapshot: org.apache.spark.sql.DataFrame = [snapshot_date: date, case_id: int]

scala> snapshot.show(20)
+-------------+-------+
|snapshot_date|case_id|
+-------------+-------+
|   2015-01-01|     12|
|   2015-01-02|     12|
|   2015-01-03|     12|
|   2015-01-04|     12|
|   2015-01-05|     12|
|   2015-01-06|     12|
|   2015-01-07|     12|
|   2015-01-08|     12|
|   2015-01-09|     12|
|   2015-01-10|     12|
|   2015-01-11|     12|
|   2015-01-02|     13|
|   2015-01-03|     13|
|   2015-01-04|     13|
|   2015-01-05|     13|
|   2015-01-06|     13|
|   2015-01-07|     13|
|   2015-01-08|     13|
|   2015-01-09|     13|
|   2015-01-10|     13|
+-------------+-------+
only showing top 20 rows


