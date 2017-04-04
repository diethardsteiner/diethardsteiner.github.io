---
layout: post
title:  "Apache Spark: Retrieving Data from a REST API and converting JSON to a Spark Dataset"
summary: This is a very short article explaining how to retrieve data from a REST API and converting the retrieved JSON data to a Spark Dataset
date: 2017-04-04
categories: Spark
tags: Spark
published: true
--- 

This is a very short article explaining how to retrieve data from a **REST API** and converting the retrieved **JSON** data to a **Spark Dataset** (using Scala). This is mainly for quick prototyping purposes; in a production environment you would have **Kafka** polling the data from the REST API and Spark picking up the data directly from Kafka then.

With this in mind, let's go ahead. In our example we will be sourcing pollution data from the **Open Data Campden** API. For testing purposes we will limit the dataset to 10 records:

```
https://opendata.camden.gov.uk/resource/83f4-6in2.json?$limit=10
```

Paste this URL into a web browser and study the returned dataset. To start off, we will just take a static example of the returned data. We create a **case class** to represent a record of the returned data and we make sure that this is working. We will use `json4s` to parse the JSON representation of the data. You can run the whole example in **Spark Shell**:

```scala
import scala.io.Source._
import org.json4s.jackson.JsonMethods.parse

implicit val formats = org.json4s.DefaultFormats


// case class to map json input

case class Location(
  `type`:String
  , coordinates:Array[String]
)
  

case class SensorData(
  bulletin_date:String
  , bulletin_date_formatted:String
  , easting:String
  , last_uploaded:String
  , latest_capture:String
  , latest_week:String
  , latitude:String
  , location:Location
  , longitude:String
  , no2_air_quality_band:String
  , no2_air_quality_index:String
  , northing:String
  , site_code:String
  , site_name:String
  , site_type:String
  , spatial_accuracy:String
  , ward_code:String
  , ward_name:String
)


//----------------- static test ----------
// making sure that case class fits the purpose

val myJson = "[{\"bulletin_date\":\"2016-07-02T21:00:00.000\",\"bulletin_date_formatted\":\"Day 184 21:00\",\"easting\":\"530529\",\"last_uploaded\":\"2016-07-02T23:05:04.000\",\"latest_capture\":\"No\",\"latest_week\":\"No\",\"latitude\":\"51.517368\",\"location\":{\"type\":\"Point\",\"coordinates\":[-0.120194,51.517368]},\"longitude\":\"-0.120194\",\"no2_air_quality_band\":\"Low\",\"no2_air_quality_index\":\"1\",\"northing\":\"181501\",\"site_code\":\"IM1\",\"site_name\":\"Camden - Holborn (inmidtown)\",\"site_type\":\"Kerbside\",\"spatial_accuracy\":\"Unknown\",\"ward_code\":\"E05000138\",\"ward_name\":\"Holborn and Covent Garden\"},{\"bulletin_date\":\"2017-01-26T03:00:00.000\",\"bulletin_date_formatted\":\"Day 026 03:00\",\"easting\":\"529885\",\"last_uploaded\":\"2017-01-26T04:05:05.000\",\"latest_capture\":\"No\",\"latest_week\":\"No\",\"latitude\":\"51.527707\",\"location\":{\"type\":\"Point\",\"coordinates\":[-0.129053,51.527707]},\"longitude\":\"-0.129053\",\"no2_air_quality_band\":\"Low\",\"no2_air_quality_index\":\"1\",\"northing\":\"182635\",\"site_code\":\"CD9\",\"site_name\":\"Camden - Euston Road\",\"site_type\":\"Roadside\",\"spatial_accuracy\":\"Unknown\",\"ward_code\":\"E05000141\",\"ward_name\":\"King's Cross\"}]"

// parse the JSON datset
val parsedData = parse(myJson).extract[Array[SensorData]]
// check a value
parsedData(1).site_code

// convert to Spark Dataset
val mySourceDataset = spark.createDataset(parsedData)

// output schema
scala> mySourceDataset.printSchema
root
 |-- bulletin_date: string (nullable = true)
 |-- bulletin_date_formatted: string (nullable = true)
 |-- easting: string (nullable = true)
 |-- last_uploaded: string (nullable = true)
 |-- latest_capture: string (nullable = true)
 |-- latest_week: string (nullable = true)
 |-- latitude: string (nullable = true)
 |-- location: struct (nullable = true)
 |    |-- type: string (nullable = true)
 |    |-- coordinates: array (nullable = true)
 |    |    |-- element: string (containsNull = true)
 |-- longitude: string (nullable = true)
 |-- no2_air_quality_band: string (nullable = true)
 |-- no2_air_quality_index: string (nullable = true)
 |-- northing: string (nullable = true)
 |-- site_code: string (nullable = true)
 |-- site_name: string (nullable = true)
 |-- site_type: string (nullable = true)
 |-- spatial_accuracy: string (nullable = true)
 |-- ward_code: string (nullable = true)
 |-- ward_name: string (nullable = true)
```

As you can see, we managed to convert the returned **JSON** representation of our pollution datsaet into valid **Spark Dataset**. Now, let's try to query the **REST API** directly:

```scala
/ ---------------- testing the REST API ---------------

val parsedData = parse(fromURL("https://opendata.camden.gov.uk/resource/83f4-6in2.json?$limit=10").mkString).extract[Array[SensorData]]
parsedData(1).site_code
val mySourceDataset = spark.createDataset(parsedData)

scala> mySourceDataset.printSchema
root
 |-- bulletin_date: string (nullable = true)
 |-- bulletin_date_formatted: string (nullable = true)
 |-- easting: string (nullable = true)
 |-- last_uploaded: string (nullable = true)
 |-- latest_capture: string (nullable = true)
 |-- latest_week: string (nullable = true)
 |-- latitude: string (nullable = true)
 |-- location: struct (nullable = true)
 |    |-- type: string (nullable = true)
 |    |-- coordinates: array (nullable = true)
 |    |    |-- element: string (containsNull = true)
 |-- longitude: string (nullable = true)
 |-- no2_air_quality_band: string (nullable = true)
 |-- no2_air_quality_index: string (nullable = true)
 |-- northing: string (nullable = true)
 |-- site_code: string (nullable = true)
 |-- site_name: string (nullable = true)
 |-- site_type: string (nullable = true)
 |-- spatial_accuracy: string (nullable = true)
 |-- ward_code: string (nullable = true)
 |-- ward_name: string (nullable = true)

scala> mySourceDataset.createOrReplaceTempView("campden_pollution_sensor_data")

scala> spark.sql("SELECT bulletin_date, ward_name, no2_air_quality_index FROM campden_pollution_sensor_data")
res9: org.apache.spark.sql.DataFrame = [bulletin_date: string, ward_name: string ... 1 more field]

scala> spark.sql("SELECT bulletin_date, ward_name, no2_air_quality_index FROM campden_pollution_sensor_data").show
+--------------------+--------------------+---------------------+
|       bulletin_date|           ward_name|no2_air_quality_index|
+--------------------+--------------------+---------------------+
|2016-07-02T21:00:...|Holborn and Coven...|                    1|
|2017-01-26T03:00:...|        King's Cross|                    1|
|2017-02-13T11:00:...|Frognal and Fitzj...|                    1|
|2017-01-30T11:00:...|        King's Cross|                    2|
|2016-12-16T23:00:...|          Bloomsbury|                    2|
|2017-02-08T18:00:...|          Bloomsbury|                    1|
|2016-09-12T11:00:...|        King's Cross|                    2|
|2016-12-25T09:00:...|        King's Cross|                    1|
|2016-07-13T05:00:...|Frognal and Fitzj...|                    1|
|2016-08-07T22:00:...|Frognal and Fitzj...|                    1|
+--------------------+--------------------+---------------------+
```

As you can see it's working nicely. At the end we start using the Spark SQL API, just for the fun of it (and real convenience). Certainly the data still needs some attention, but now that the dataset is available as a Spark Dataset, we have got all the power of Spark available to make most out of this data!