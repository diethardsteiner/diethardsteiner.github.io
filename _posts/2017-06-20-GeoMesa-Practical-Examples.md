---
layout: post
title: "Big Data Geospatial Analysis with Apache Spark, GeoMesa and Accumulo - Part 3: Practical Examples"
summary: This article walks you through practical GeoMesa examples.
date: 2017-06-20
categories: Geospatial
tags: Geospatial, Spark
published: true
---  

In **Part 1** we had a look at how to set up **GeoMesa** and **Part 2** explained the GeoMesa and Accumulo basics. **Part 3** focuses on some practical examples. Note that some sections were copied directly from the GeoMesa and other help pages. My main aim was to provide a consistent overview.

The code for the examples discussed in this post can be found [here](https://github.com/diethardsteiner/diethardsteiner.github.io/tree/master/sample-files/gis/geomesa).

## Sample Data

**GeoMesa** provides a utility called `download-data.sh` to download sample data. Use the following command line argument to download a specific source:

- `gdelt` for [GDELT](http://gdeltproject.org/): The **Global Database of Events, Language, and Tone** monitors the world's news media from nearly every corner of every country in print, broadcast, and web formats, in over 100 languages,
every moment of every day. [Prepackaged Converter](
- `osm-gpx` for [OpenStreetMap GPX Data](http://wiki.openstreetmap.org/wiki/Planet.gpx): The GPS traces are a series of latitude/longitude pairs collected by OSM or uploaded by users. The datasets were last updated in 2013. [Prepackaged Converter](http://www.geomesa.org/documentation/user/convert/premade/osm-gpx.html)
- `tdrive` for [T-Drive](http://research.microsoft.com/apps/pubs/?id=152883): T-Drive is a project of Microsoft Research Asia. The overall project collected GPS tracks from over 30,000 taxis in Beijing for three months. These data were used to demonstrate a more efficient routing system. [Prepackaged Converter](http://www.geomesa.org/documentation/user/convert/premade/tdrive.html)
- `geonames` for [GeoNames](http://www.geonames.org/): GeoNames is a geographical database containing over 10 million geographical names and over 9 million unique features. All features are classified as one of nine feature codes and then again sub-categorized into one of 645 feature codes. This data is freely available directly from GeoNames’s website. [Prepackaged Converter](http://www.geomesa.org/documentation/user/convert/premade/geonames.html)

Each of these data source scripts come with a [Prepackaged Converter](http://www.geomesa.org/documentation/user/convert/premade/index.html) to transform the data into the required target format.

There are a few other sample data sources available, like Twitter and NYC Taxi data, however, ingesting them involves a little bit more more, so please consult the documentation.

Moving forward, we will be using the GDELT data for our exercises.

## Importing Data

### How to ingest CSV files

[This](http://www.geomesa.org/documentation/user/convert/premade/gdelt.html#global-database-of-events-language-and-tone-gdelt) is a good example.

Run the following to get an overview of the ingest command:

```
$ geomesa help ingest
```

Example:

```
$ cd $GEOMESA_ACCUMULO_HOME/bin
$ ./download-data.sh gdelt
$ cd data/gdelt
$ unzip 20170105.export.CSV.zip
$ cd ../..
$ geomesa ingest --help
$ geomesa ingest -u root -p password -c myNamespace.gdelt -s gdelt -C gdelt data/gdelt/20170105.export.CSV
# if it gets stuck the data path is probably wrong
# do not run: same but being more explicit with instance name
$ geomesa ingest -u root -p password -i BISSOL_CONSULTING -c myNamespace.gdelt -s gdelt -C gdelt data/gdelt/20170105.export.CSV
```

> **Note**: If the ingesting progress is 0% after a while it is very likely that the path of the input file is not correct.

`geomesa ingest` options:

parameter | description
----------|---------
`-c` | catalog / **table** name, catalog is a synonym for table here, so just specify the fully qualified table name: `<namespace>.<table>`
`-i` | instance name
`-s` | SimpleFeatureType specification as a GeoTools spec string, SFT config, or file with either
`-C` | GeoMesa converter

Any errors in ingestion will be logged to `$GEOMESA_ACCUMULO_HOME/logs`.

**Verify** that the required tables exist, either via the command line (as shown below) or the **Accumulo web UI** (just click on the Table link). 

You should see 5 tables:

- `myNamespace.gdelt`: This is the catalog table, where **metadata** concerning all of the features that share this **table base name** will be stored.
- `myNamespace.gdelt_gdelt_records_v2`
- `myNamespace.gdelt_gdelt_z2_v3`
- `myNamespace.gdelt_gdelt_z3_v4`
- `myNamespace.gdelt_stats`

> **Note**: Assuming that your features share tables, which is the default, then you would expect the total number of tables to follow approximately 1 + 4F, where F is the number of feature types you ingest: one catalog table, and four feature-specific data tables.

```bash
accumulo shell --help
accumulo shell -u root -p password
# you will be prompted for password, leave empty, hit enter
# type `help` to see a list of commands
tables
namespaces
# show all tables for certain namespace
tables -ns myNamespace
# show table content
scan -t myNamespace.gdelt
scan -t myNamespace.gdelt_gdelt_records_v2
scan -t myNamespace.gdelt_gdelt_z2_v3
scan -t myNamespace.gdelt_gdelt_z3_v4
exit
# list all known feature types in a GeoMesa catalog:
geomesa get-type-names -u root -c myNamespace.gdelt
# describe feature
geomesa describe-schema -u root -c myNamespace.gdelt -f gdelt
```

Another exercise in **Accumulo shell**:

```
root@BISSOL_CONSULTING> scan -t myNamespace.gdelt
gdelt~attributes : []    globalEventId:String,eventCode:String,eventBaseCode:String,eventRootCode:String,isRootEvent:Integer,actor1Name:String,actor1Code:String,actor1CountryCode:String,actor1GroupCode:String,actor1EthnicCode:String,actor1Religion1Code:String,actor1Religion2Code:String,actor2Name:String,actor2Code:String,actor2CountryCode:String,actor2GroupCode:String,actor2EthnicCode:String,actor2Religion1Code:String,actor2Religion2Code:String,quadClass:Integer,goldsteinScale:Double,numMentions:Integer,numSources:Integer,numArticles:Integer,avgTone:Double,dtg:Date,*geom:Point:srid=4326;geomesa.index.dtg='dtg',geomesa.table.sharing='false',geomesa.indices='z3:4:3,z2:3:3,records:2:3'
gdelt~id : []    \x01
gdelt~stats-date : []    2017-05-09T18:40:56.314Z
gdelt~table.records.v2 : []    myNamespace.gdelt_gdelt_records_v2
gdelt~table.z2.v3 : []    myNamespace.gdelt_gdelt_z2_v3
gdelt~table.z3.v4 : []    myNamespace.gdelt_gdelt_z3_v4
root@BISSOL_CONSULTING> scan -t myNamespace.gdelt -r gdelt~stats-date
gdelt~stats-date : []    2017-05-09T18:40:56.314Z
```

The last command returns the row with the key `gdelt~stats-date`. The command just before this one lists all the rows of the `myNamespace.gdelt` table. It is a bit difficult to read (since the last column, the value field, is of varying length), but the first column is the key (e.g. `gdelt~attributes`), the next column holds the column attributes (empty in the case, donated by `[]`)



### Ingesting Data with MapReduce

Take a look at this example: [Map-Reduce Ingest of GDELT](http://www.geomesa.org/documentation/tutorials/geomesa-examples-gdelt.html)

There is a section further down explaining how to ingest the data with Spark. 

## Using the GeoMesa Accumulo Spark Runtime


You have to register the **Kyro Serialiser** with spark either dynamically (as shown [here](https://ogirardot.wordpress.com/2015/01/09/changing-sparks-default-java-serialization-to-kryo/))  or via `conf/spark-defaults.conf`. 

If you don't have a Spark conf file already, create one based on the template:

```
cp conf/spark-defaults.conf.template conf/spark-defaults.conf
vi conf/spark-defaults.conf
```

And add the settings listed below:

```
# GEOMESA SPECIFIC SETTINGS
spark.serializer    org.apache.spark.serializer.KryoSerializer
spark.kryo.registrator    org.locationtech.geomesa.spark.GeoMesaSparkKryoRegistrator
spark.kryo.registrationRequired    false
```

[Source](http://www.geomesa.org/documentation/user/spark/accumulo_spark_runtime.html)

The `geomesa-accumulo-spark-runtime` module (found in the geomesa-accumulo directory in the **GeoMesa** source distribution) provides a shaded JAR with all of the dependencies for **Spark** and **Spark SQL** analysis for data stored in **GeoMesa Accumulo**, including the GeoMesa Accumulo data store, the GeoMesa Spark and GeoMesa Spark SQL modules, and the `AccumuloSpatialRDDProvider`.

The shaded JAR is included in the `dist/spark` directory of the GeoMesa Accumulo binary distribution. **This JAR may be passed as an argument to the Spark command line tools**, or to Jupyter running the Toree kernel, without having to also include the other dependencies required to run GeoMesa or Accumulo.

For example, the JAR can be used with the `--jars` switch of the spark-shell command:

```bash
$ spark-shell --jars /path/to/geomesa-accumulo-spark-runtime_2.11-$VERSION.jar
# e.g.
$ spark-shell --jars /home/dsteiner/apps/geomesa-accumulo_2.11-1.3.1/dist/spark/geomesa-accumulo-spark-runtime_2.11-1.3.1.jar
```

You should see a logging message loading the shaded JAR as the Spark console comes up (as long as you set up `log4j` to log on `INFO` level):

```bash
...
17/01/16 14:31:21 INFO SparkContext: Added JAR file:/path/to/geomesa-accumulo-spark-runtime_2.11-$VERSION.jar at http://192.168.3.14:40775/jars/geomesa-accumulo-spark-runtime_2.11-$VERSION.jar with timestamp 1484595081362
...
```

You also have to import following packages:

```scala
import java.text.SimpleDateFormat
import org.apache.hadoop.conf.Configuration
import org.apache.spark.{SparkConf, SparkContext}
import org.geotools.data.{DataStoreFinder, Query}
import org.geotools.factory.CommonFactoryFinder
import org.locationtech.geomesa.accumulo.data._
import org.locationtech.geomesa.spark.GeoMesaSpark
import org.opengis.filter.Filter

import scala.collection.JavaConversions._
```


## Basic CountByDay Example: Sourcing Data from GeoMesa Accumulo and analysing data

[Source](http://www.geomesa.org/documentation/tutorials/spark.html)

Prerequisite: Load sample data as described in previous section [How to ingest CSV files]().

> **Note**: `GeoMesaSpark` used to be in [geomesa-accumulo-compute](https://github.com/locationtech/geomesa/blob/master/geomesa-accumulo/geomesa-accumulo-compute/src/main/scala/org/locationtech/geomesa/compute/spark/GeoMesaSpark.scala0) (which is marked as myNamespace.gdelt_gdelt_records_v2deprecated) and seems to be now in [geomesa-spark/geomesa-spark-core](https://github.com/locationtech/geomesa/blob/master/geomesa-spark/geomesa-spark-core/src/main/scala/org/locationtech/geomesa/spark/GeoMesaSpark.scala). The package name changed from `org.locationtech.geomesa.compute.spark` to `org.locationtech.geomesa.spark`.


### Project Version

#### SBT Build file

Fully working `build.sbt`:

```scala
name := "GeoMesaSparkExample"

version := "0.1"

scalaVersion := "2.11.8"

// Add necessary resolvers
resolvers ++= Seq(
  "locationtech-releases" at "https://repo.locationtech.org/content/groups/releases",
  "boundlessgeo" at "https://repo.boundlessgeo.com/main",
  "osgeo" at "http://download.osgeo.org/webdav/geotools",
  "conjars.org" at "http://conjars.org/repo",
  "media.javax" at "http://maven.geotoolkit.org"
)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "2.0.2" % "provided"
  , "org.apache.spark" %% "spark-catalyst" % "2.0.2" % "provided"
  , "org.apache.spark" %% "spark-sql" % "2.0.2" % "provided"
/**  , "org.apache.spark" %% "spark-yarn" % "2.0.2" % "provided" **/
  , "org.locationtech.geomesa" %% "geomesa-accumulo-datastore" % "1.3.1"
  , "org.locationtech.geomesa" %% "geomesa-accumulo-spark" % "1.3.1"
  , "org.locationtech.geomesa" %% "geomesa-spark-sql" % "1.3.1"
  , "ch.qos.logback" % "logback-classic" % "1.1.7"
  , "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
  , "org.apache.accumulo" % "accumulo-core" % "1.7.3"
)
```


If you get this error:

```
[error] missing or invalid dependency detected while loading class file 'GeoMesaDataStore.class'.
[error] Could not access type LazyLogging in value com.typesafe.scalalogging,
[error] because it (or its dependencies) are missing. Check your build definition for
[error] missing or conflicting dependencies. (Re-run with `-Ylog-classpath` to see the problematic classpath.)
```

The solution is to add dependencies as shown [here](https://github.com/typesafehub/scala-logging)

#### Scala code

```scala
package examples

import java.text.SimpleDateFormat

import org.apache.hadoop.conf.Configuration
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.geotools.data.{DataStoreFinder, Query}
import org.geotools.factory.CommonFactoryFinder
import org.geotools.filter.text.ecql.ECQL
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore
import org.locationtech.geomesa.spark.GeoMesaSpark
import org.opengis.feature.simple.SimpleFeature

import scala.collection.JavaConversions._

object CountByDay {

  val params = Map(
    "instanceId" -> "BISSOL_CONSULTING",
    // zookeeper info can be found in ACCUMULO_HOME/conf/accumulo-site.xml
    "zookeepers" -> "localhost:2181",
    "user"       -> "root",
    "password"   -> "password",
    // no authentication required for local dev env setup
    //"auths"      -> "USER,ADMIN",
    "tableName"  -> "myNamespace.gdelt"
    )

  // see geomesa-tools/conf/sfts/gdelt/reference.conf
  val typeName = "gdelt"
  val geom     = "geom"
  val date     = "dtg"

  val bbox   = "-80, 35, -79, 36"
  val during = "2014-01-01T00:00:00.000Z/2014-01-31T12:00:00.000Z"

  // val filter = s"bbox($geom, $bbox) AND $date during $during"
  val filter = s"bbox($geom, $bbox)"

  def main(args: Array[String]) {
    // Get a handle to the data store
    val ds = DataStoreFinder.getDataStore(params).asInstanceOf[AccumuloDataStore]

    // Construct a CQL query to filter by bounding box
    val q = new Query(typeName, ECQL.toFilter(filter))

    // Configure Spark
    val conf = new SparkConf().setAppName("testSpark")
    val sc = SparkContext.getOrCreate(conf)

    // Get the appropriate spatial RDD provider
    val spatialRDDProvider = GeoMesaSpark(params)

    // Get an RDD[SimpleFeature] from the spatial RDD provider
    val rdd = spatialRDDProvider.rdd(new Configuration, sc, params, q)

    // Collect the results and print
    countByDay(rdd).collect().foreach(println)
    println("\n")

    ds.dispose()
  }

  // extract the SQLDATE from each SimpleFeature and truncating it to the day resolution
  def countByDay(rdd: RDD[SimpleFeature], dateField: String = "dtg") = {
    val dayAndFeature = rdd.mapPartitions { iter =>
      val df = new SimpleDateFormat("yyyyMMdd")
      val ff = CommonFactoryFinder.getFilterFactory2
      val exp = ff.property(dateField)
      iter.map { f => (df.format(exp.evaluate(f).asInstanceOf[java.util.Date]), f) }
    }
    dayAndFeature.map( x => (x._1, 1)).reduceByKey(_ + _)
  }
}
```

#### Build Fat JAR

Next let's set up the config to compiled the code/create a **fat jar** using **sbt assembly**:

```
echo 'addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")' > project/assembly.sbt
```

Add this to the end of your `build.sbt` file to **avoid merge conflicts**:

```
assemblyMergeStrategy in assembly := {
  case path => {
    val strategy = (assemblyMergeStrategy in assembly).value(path)
    if (strategy == MergeStrategy.deduplicate) {
      MergeStrategy.first
    } else {
      strategy
    }
  }
}
```

Next build the fat jar:

```
sbt clean assembly
```

The jar will be stored in following location:

```
target/scala-2.11/GeoMesaSparkExample-assembly-0.1.jar
```

#### Run Job

Start up your local Spark server:

```
cd $SPARK_HOME
# make sure `localhost` is listed in the config
vi conf/slaves
# then start spark standalone server
./sbin/start-all.sh
```

Next let's run the job:

```
# back to your geomesa project dir
cd -
# submit job
spark-submit --master local[4] \
  --class examples.CountByDay \
  target/scala-2.11/GeoMesaSparkExample-assembly-0.1.jar
```


### Spark Shell Version

Builds on the data ingested before (see sections further up).

Start **Spark Shell** with the **GeoMesa Accumulo runtime**:

```bash
spark-shell --jars /home/dsteiner/apps/geomesa-accumulo_2.11-1.3.1/dist/spark/geomesa-accumulo-spark-runtime_2.11-1.3.1.jar
```

Once **Spark-Shell** is running, you have to import all the required packages:

```scala
import org.geotools.data.{DataStoreFinder, Query}
import org.geotools.factory.CommonFactoryFinder
import org.geotools.filter.text.ecql.ECQL
//import org.opengis.filter.Filter
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore
import org.locationtech.geomesa.spark.GeoMesaSpark
// org.locationtech.geomesa.compute.spark.GeoMesaSpark
// seems to be deprecated, see https://github.com/locationtech/geomesa/blob/master/geomesa-accumulo/geomesa-accumulo-compute/src/main/scala/org/locationtech/geomesa/compute/spark/GeoMesaSpark.scala

import org.opengis.feature.simple.SimpleFeature

import java.text.SimpleDateFormat
import scala.collection.JavaConversions._
import org.apache.hadoop.conf.Configuration
```

Define **connection parameters**:

```scala
val params = Map(
  "instanceId" -> "BISSOL_CONSULTING",
  // zookeeper info can be found in ACCUMULO_HOME/conf/accumulo-site.xml
  "zookeepers" -> "localhost:2181",
  "user"       -> "root",
  "password"   -> "password",
  // no authentication required for local dev env setup
  //"auths"      -> "USER,ADMIN",
  "tableName"  -> "myNamespace.gdelt"
  )
```

Define an **ECQL** filter used to select a subset of GDELT data from the **GeoMesa Accumulo data store**. The value of during should also be edited to match the range of GDELT data that you have ingested.

```scala
val featureName = "gdelt"
val geom     = "geom"
val date     = "dtg"

val bbox   = "-80, 35, -79, 36"
val during = "2017-01-05T00:00:00.000Z/2017-01-06T12:00:00.000Z"

// val filter = s"bbox($geom, $bbox) AND $date during $during"
val filter = s"bbox($geom, $bbox)"
// Get a handle to the data store
val ds = DataStoreFinder.getDataStore(params).asInstanceOf[AccumuloDataStore]

// Construct a CQL query to filter by bounding box
val q = new Query(featureName, ECQL.toFilter(filter))

// Get the appropriate spatial RDD provider
val spatialRDDProvider = GeoMesaSpark(params)

// Get an RDD[SimpleFeature] from the spatial RDD provider
// this one requires import org.apache.hadoop.conf.Configuration
// THIS EXTRACTS THE KEYS ONLY
val rdd = spatialRDDProvider.rdd(new Configuration, sc, params, q)

// IF IT GETS STUCK HERE: either connection details are wrong
// or you are not using the dedicated Accumulo namespace
// which has all the geomesa dependencies.

rdd.take(2)
/** 
result should look similar to this one:
res0: Array[org.opengis.feature.simple.SimpleFeature] = Array(ScalaSimpleFeature:31a70f3f6e748374b22f24e0094e76b4, ScalaSimpleFeature:51117014214fa48a009c33c37e2784d7)

you can find these keys via Accumulo as well, e.g.:
scan -t myNamespace.gdelt_gdelt_records_v2 -r 51117014214fa48a009c33c37e2784d7
**/

// extract the date from each SimpleFeature and truncating it to the day resolution
val dayAndFeature = rdd.mapPartitions { iter =>
  val df = new SimpleDateFormat("yyyyMMdd")
  val ff = CommonFactoryFinder.getFilterFactory2
  // extract date, "dtg" is the name of the date field
  val exp = ff.property("dtg")
  iter.map { f => (df.format(exp.evaluate(f).asInstanceOf[java.util.Date]), f) }
}

// group by the day and count up the number of events in each group.

val countByDay = dayAndFeature.map( x => (x._1, 1)).reduceByKey(_ + _)

countByDay.collect().foreach(println)
```

The expected output is something like this:

```
(20170104,1)
(20161206,7)
(20160106,12)
(20170105,566)
(20161229,10)
```

## Spatial Density Example

Based on [this example](http://www.geomesa.org/documentation/tutorials/spark.html#parallel-computation-of-spatial-event-densities): "We compute densities of our feature by discretizing the spatial domain and counting occurrences of the feature in each grid cell. We use **GeoHashes** as our discretization of the world so that we can configure the resolution of our density by setting the number of bits in the GeoHash."

In the `geomesa-tutorials` repo there is also a **ShallowJoin** example available (`geomesa-examples-spark/src/main/scala/com/example/geomesa/spark/ShallowJoin.scala`).

For **GeoHashes** to work you have to import following packages:

```
import org.locationtech.geomesa
import com.vividsolutions.jts.geom.Point
```

This time round we will not use Spark Shell. 

Scala code:

```scala
package examples

import java.text.SimpleDateFormat

import org.apache.hadoop.conf.Configuration
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.geotools.data.{DataStoreFinder, Query}
import org.geotools.factory.CommonFactoryFinder
import org.geotools.filter.text.ecql.ECQL
import org.locationtech.geomesa
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore
import org.locationtech.geomesa.spark.GeoMesaSpark
import com.vividsolutions.jts.geom.Point
import org.opengis.feature.simple.SimpleFeature

import scala.collection.JavaConversions._

object SpatialDensities {

  val params = Map(
    "instanceId" -> "BISSOL_CONSULTING",
    // zookeeper info can be found in ACCUMULO_HOME/conf/accumulo-site.xml
    "zookeepers" -> "localhost:2181",
    "user"       -> "root",
    "password"   -> "password",
    // no authentication required for local dev env setup
    //"auths"      -> "USER,ADMIN",
    "tableName"  -> "myNamespace.gdelt"
    )


  val typeName = "gdelt"
  val geom     = "geom"
  val bbox   = "-180, -90, 180, 90"
  val cartoClass = "EPSG:4326"

  //val f = bbox("geom", -180, -90, 180, 90, "EPSG:4326")
  //  val filter = s"""bbox($geom, $bbox, $cartoClass)"""
  val filter = s"bbox($geom, $bbox)"

  def main(args: Array[String]) {
  
    // Get a handle to the data store
    val ds = DataStoreFinder.getDataStore(params).asInstanceOf[AccumuloDataStore]

    // Construct new query
    //val q = new Query("gdelt", f)
    val q = new Query(typeName, ECQL.toFilter(filter))
    
    // Configure Spark
    val conf = new SparkConf().setAppName("testSpark")
    val sc = SparkContext.getOrCreate(conf)

    // Get the appropriate spatial RDD provider
    val spatialRDDProvider = GeoMesaSpark(params)

    val queryRDD = spatialRDDProvider.rdd(new Configuration, sc, params, q)

    // Project (in the relational sense) the SimpleFeature to a 2-tuple of (GeoHash, 1)
    val discretized = queryRDD.map {
      f => (
        geomesa.utils.geohash.GeoHash(f.getDefaultGeometry.asInstanceOf[Point], 25)
        , 1
      )
    }

    // group by grid cell and count the number of features per cell.
    val density = discretized.reduceByKey(_ + _)

    density.collect.foreach(println)

    ds.dispose()
  }
}
```

Build fat jar as shown in previous section.

Run job:

```bash
sbt clean assembly
# submit job
spark-submit --master local[4] \
  --class examples.SpatialDensities \
  target/scala-2.11/GeoMesaSparkExample-assembly-0.1.jar
```

Expected sample **output**:

```
(GeoHash(145.96435546875,-36.54052734375,BoundingBox(POINT (145.9423828125 -36.5625),POINT (145.986328125 -36.5185546875)),BitSet(0, 2, 3, 4, 9, 10, 11, 12, 14, 16, 18, 24),25,None),1)
(GeoHash(-80.39794921875,37.15576171875,BoundingBox(POINT (-80.419921875 37.1337890625),POINT (-80.3759765625 37.177734375)),BitSet(1, 2, 5, 7, 10, 11, 12, 16, 17, 18, 19, 22, 23),25,None),1)
```

## Spark SQL with GeoMesa Example

- [SparkSQL — GeoMesa 1.3.1 Manuals](http://www.geomesa.org/documentation/user/spark/sparksql.html)
- [SparkSQL Functions — GeoMesa 1.3.1 Manuals](http://www.geomesa.org/documentation/user/spark/sparksql_functions.html)

With the help of the **GeoMesa Spark SQL** module analysing geo-spatial data got a lot easier. Here is a quick demo on how this is done (again based on the example from the docu, although slightly adjusted to work with our previous sample dataset):

Add this to your **SBT build** dependencies:

```
"org.locationtech.geomesa" %% "geomesa-spark-sql" % "1.3.1"
```

The code:

```scala
import java.text.SimpleDateFormat

import org.apache.hadoop.conf.Configuration
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}
import org.geotools.data.{DataStoreFinder, Query}
import org.geotools.factory.CommonFactoryFinder
import org.geotools.filter.text.ecql.ECQL
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore
import org.locationtech.geomesa.spark.GeoMesaSpark
import org.opengis.feature.simple.SimpleFeature

import scala.collection.JavaConversions._

object SparkSQLExample {

  val params = Map(
    "instanceId" -> "BISSOL_CONSULTING",
    // zookeeper info can be found in ACCUMULO_HOME/conf/accumulo-site.xml
    "zookeepers" -> "localhost:2181",
    "user"       -> "root",
    "password"   -> "password",
    // no authentication required for local dev env setup
    //"auths"      -> "USER,ADMIN",
    "tableName"  -> "myNamespace.gdelt"
    )

  def main(args: Array[String]) {

    // Create SparkSession
    val sparkSession = SparkSession.builder()
      .appName("testSpark")
      .config("spark.sql.crossJoin.enabled", "true")
      .master("local[*]")
      .getOrCreate()

    // Create DataFrame using the "geomesa" format
    val dataFrame = sparkSession
      .read
      // register the GeoMesa SparkSQL data source
      .format("geomesa")
      .options(params)
      // tell GeoMesa to use the feature type named gdelt
      .option("geomesa.feature", "gdelt") /** typeName from prev exercise **/
      .load()

    dataFrame.createOrReplaceTempView("gdelt")

    // Query against the "chicago" schema
    val sqlQuery = "select * from gdelt where st_contains(st_makeBBOX(-80.0, 35.0, -79.0, 36.0), geom)"
    val resultDataFrame = sparkSession.sql(sqlQuery)

    resultDataFrame.show
  }
}
```

For a detailed explanation read the official docu: [SparkSQL — GeoMesa 1.3.1 Manuals](http://www.geomesa.org/documentation/user/spark/sparksql.html)


Create the fat jar and run the job:

```bash
sbt clean assembly
# submit job
spark-submit --master local[4] \
  --class examples.SparkSQLExample \
  target/scala-2.11/GeoMesaSparkExample-assembly-0.1.jar
```

## SBT Dependencies

The location of the GeoMesa Maven Repository is:

```
https://repo.locationtech.org/content/groups/releases/org/locationtech/geomesa/
# or
https://repo.locationtech.org/content/repositories/releases/org/locationtech/geomesa/
```

The [GeoMesa Github](https://github.com/locationtech/geomesa) README file explains how to configure the dependencies.


`build.sbt`:

```

// Add necessary resolvers for GeoMesa
resolvers ++= Seq(
  "locationtech-releases" at "https://repo.locationtech.org/content/groups/releases",
  "boundlessgeo" at "https://repo.boundlessgeo.com/main",
  "osgeo" at "http://download.osgeo.org/webdav/geotools",
  "conjars.org" at "http://conjars.org/repo"
)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided"
  , "org.apache.spark" %% "spark-mllib" % sparkVersion % "provided"
  , "org.apache.spark" %% "spark-sql" % sparkVersion % "provided"
  //, "org.apache.hadoop" % "hadoop-client" % "2.8.0" % "provided"
  // uncommented above, see also http://stackoverflow.com/questions/30860551/in-sbt-how-can-we-specify-the-version-of-hadoop-on-which-spark-depends
  , "org.locationtech.geomesa" %% "geomesa-accumulo-datastore" % "1.3.1"       
  , "org.locationtech.geomesa" %% "geomesa-accumulo-compute" % "1.3.1"
  // , "org.locationtech.geomesa" %% "geomesa-utils" % "1.3.1"

)
```

> **Note**: Browse [the GeoMesa Repo](https://repo.locationtech.org/content/groups/releases) in your web browser to see which packages are available.

You might get following error:

```
FAILED DOWNLOADS javax.media#jai_core;1.1.3!jai_core.jar
sbt.ResolveException: download failed: javax.media#jai_core;1.1.3!jai_core.jar
```

[Solution](http://stackoverflow.com/questions/26993105/i-get-an-error-downloading-javax-media-jai-core1-1-3-from-maven-central)

Add this to the SBT resolvers:

```
"media.javax" at "http://maven.geotoolkit.org"
```

# Error and Solutions

## Accumulo Web UI Tables Monitor: StatsCombiner not found

Error msg in the Web UI:

```
java.lang.ClassNotFoundException: org.locationtech.geomesa.accumulo.data.stats.StatsCombiner
```

[Source](https://dev.locationtech.org/mhonarc/lists/geomesa-users/msg02092.html)

In the **Accumulo Shell** check if the classpath is set correctly:

```
# show all scopes and classpaths
config -f context.classpath
# show classpath for a specific namespace
config -ns myNamespace -f classpath
```

If the jar is not listed in the `value` field, then your setup is not correct.

If the `setup-namespace.sh` script isn't working quite right, you can also follow the manual steps for configuring the classpath as outlined in the docs:

http://www.geomesa.org/documentation/user/accumulo/install.html#manual-install

or

http://www.geomesa.org/documentation/user/accumulo/install.html#namespace-install

Correct output should be:

```
root@BISSOL_CONSULTING> config -f context.classpath
-----------+----------------------------------------------+----------------------------
SCOPE      | NAME                                         | VALUE
-----------+----------------------------------------------+----------------------------
site       | general.vfs.context.classpath.myNamespace .. | 
system     |    @override ............................... | hdfs://localhost:8020/accumulo/classpath/myNamespace/[^.].*.jar
-----------+----------------------------------------------+----------------------------

root@BISSOL_CONSULTING> config -ns myNamespace -f classpath
-----------+----------------------------+-----------------
SCOPE      | NAME                       | VALUE
-----------+----------------------------+-----------------
default    | table.classpath.context .. | 
namespace  |    @override ............. | myNamespace
-----------+----------------------------+-----------------
```

## Spark Shell: ScalaSimpleFeature Object not serializable

Error msg:

```
object not serializable (class: org.locationtech.geomesa.features.ScalaSimpleFeature
```

Solution:

[Source](https://dev.locationtech.org/mhonarc/lists/geomesa-users/msg01803.html)

My attempt is below in spark 1.6.1, notice that I have re-arranged a few things: I am specifying the --master and --name on the spark-shell command. I do not initialize a new the spark context either.

Another thing to consider is the `conf/spark-defaults.conf`. In my particular case, I have the following defined:

```
# GEOMESA SPECIFIC SETTINGS
spark.serializer    org.apache.spark.serializer.KryoSerializer
spark.kryo.registrator    org.locationtech.geomesa.spark.GeoMesaSparkKryoRegistrator
spark.kryo.registrationRequired    false
```

DS:

```
# if you don't have a spark conf already, create one based
# on the template
cp conf/spark-defaults.conf.template conf/spark-defaults.conf
vi conf/spark-defaults.conf
# add the settings listed above
```

or define dynamically like shown [here](https://ogirardot.wordpress.com/2015/01/09/changing-sparks-default-java-serialization-to-kryo/)

-- end DS comments

Here's the outline of what I did below. I hope it helps.

```
$ spark-shell --master local[2] --name "localtest" --jars ${GEOMESA_SRC}/geomesa-compute-1.2.3-shaded.jar


:paste

import java.text.SimpleDateFormat
import org.apache.hadoop.conf.Configuration
import org.apache.spark.{SparkConf, SparkContext}
import org.geotools.data.{DataStoreFinder, Query}
import org.geotools.factory.CommonFactoryFinder
import org.locationtech.geomesa.accumulo.data._
import org.locationtech.geomesa.spark.GeoMesaSpark
import org.opengis.filter.Filter

import scala.collection.JavaConversions._

val feature = "flightaware"
val params = Map(
  "instanceId" -> "tcloud",
  "zookeepers" -> "tzoo1:2181,tzoo2:2181,tzoo3:2181",
  "user" -> "root",
  "password" -> "secret",
  "tableName" -> "geomesa122.jbrown" )
val ds = DataStoreFinder.getDataStore(params).asInstanceOf[AccumuloDataStore]

import org.geotools.filter.text.ecql.ECQL
val filter = "BBOX(geom, -16.0, 35.0,-9.0, 53.0) AND dtg DURING 2016-07-26T00:00:00.000Z/2016-07-26T02:00:00.000Z"
val q = new Query(feature, ECQL.toFilter(filter))

GeoMesaSpark.init(sc.getConf, ds)
val conf = new Configuration

val queryRDD = org.locationtech.geomesa.compute.spark.GeoMesaSpark.rdd(conf, sc, params, q)
queryRDD.first

<CTRL + D>
res1: org.opengis.feature.simple.SimpleFeature = ScalaSimpleFeature:some_feature_id
```

## No such method error: sendBaseOneway

Error msg:

```
java.lang.NoSuchMethodError: org.apache.accumulo.core.tabletserver.thrift.TabletClientService$Client.sendBaseOneway
```

[Solution](https://dev.locationtech.org/mhonarc/lists/geomesa-users/msg01922.html)

Basically, versions of the libthrift library are out of sync. 

It is a mismatch in the Accumulo version.  GeoMesa 1.3.x depends on Accumulo 1.7.x.  To get around this, you'll need to build GeoMesa locally using the Accumulo 1.8 profile (`mvn clean install -Paccumulo-1.8`) and you'll need to make sure that sbt picks up the artifacts which you have built locally.

As highlighted in [A note about Accumulo 1.8](http://www.geomesa.org/documentation/user/accumulo/install.html#a-note-about-accumulo-1-8): "GeoMesa supports Accumulo 1.8 when built with the accumulo-1.8 profile. Accumulo 1.8 introduced a dependency on libthrift version 0.9.3 which is not compatible with Accumulo 1.7/libthrift 0.9.1. The default supported version for GeoMesa is Accumulo 1.7.x and the published jars and distribution artifacts reflect this version. To upgrade, build locally using the accumulo-1.8 profile."