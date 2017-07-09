---
layout: post
title: "Big Data Geospatial Analysis with Apache Spark, GeoMesa and Accumulo - Part 4: Ingesting Data with Spark SQL"
summary: This article walks you through practical GeoMesa examples.
date: 2017-07-01
categories: Geospatial
tags: Geospatial, Spark
published: true
---  

## Ingesting Data with Spark

This example demonstrates how to load data into **GeoMesa Accumulo** using **Spark SQL**. The code for the examples discussed in this post can be found [here](https://github.com/diethardsteiner/diethardsteiner.github.io/tree/master/sample-files/gis/geomesa).

Add this **dependency** to your `build.sbt`:

```
"org.geotools" % "gt-epsg-hsql" % "17.1"
```

Let's first set up a new **Accumulo namespace** for this import:

```bash
accumulo shell -u root -p password
> createnamespace sparkImportTest
> grant NameSpace.CREATE_TABLE -ns sparkImportTest -u root
> config -s general.vfs.context.classpath.sparkImportTest=hdfs://localhost:8020/accumulo/classpath/sparkImportTest/[^.].*.jar
> config -ns sparkImportTest -s table.classpath.context=sparkImportTest
> exit
```

Next upload the **GeoMesa** dependencies to **HDFS** (adjust to your version and setup):

```bash
export VERSION=1.3.2-SNAPSHOT
#export VERSION=1.3.1
cd $GEOMESA_ACCUMULO_HOME
hdfs dfs -mkdir -p /accumulo/classpath/sparkImportTest
hdfs dfs -copyFromLocal dist/accumulo/geomesa-accumulo-distributed-runtime_2.11-$VERSION.jar /accumulo/classpath/sparkImportTest 
hdfs dfs -ls /accumulo/classpath/sparkImportTest
```

Restart **Accumulo**.

The code to ingest the file looks like this (essential section shown only - full example available on my Github repo):


Upfront we might want to create the **Geomesa feature** (schema), in case it doesn't exist yet:

```scala
val dsParams = Map(
  "instanceId"  -> "BISSOL_CONSULTING",
  "zookeepers"  -> "127.0.0.1:2181",
  "user"        -> "root",
  "password"    -> "password",
  "tableName"   -> "sparkImportTest.gdelt_new"
)

val featureName = "event"

// Create GeoMesa Feature
val geoMesaSchema = Lists.newArrayList(
  "GlobalEventId:Integer",
  "SqlDate:Date",
  "MonthYear:Integer",
  "Year:Integer",
  "FractionDate:Float",
  "Actor1Code:String",
  "Actor1Name:String",
  "Actor1CountryCode:String",
  "Actor1KnownGroupCode:String",
  "Actor1EthnicCode:String",
  "Actor1Religion1Code:String",
  "Actor1Religion2Code:String",
  "Actor1Type1Code:String",
  "Actor1Type2Code:String",
  "Actor1Type3Code:String",
  "Actor2Code:String",
  "Actor2Name:String",
  "Actor2CountryCode:String",
  "Actor2KnownGroupCode:String",
  "Actor2EthnicCode:String",
  "Actor2Religion1Code:String",
  "Actor2Religion2Code:String",
  "Actor2Type1Code:String",
  "Actor2Type2Code:String",
  "Actor2Type3Code:String",
  "IsRootEvent:Integer",
  "EventCode:String",
  "EventBaseCode:String",
  "EventRootCode:String",
  "QuadClass:Integer",
  "GoldsteinScale:Float",
  "NumMentions:Integer",
  "NumSources:Integer",
  "NumArticles:Integer",
  "AvgTone:Float",
  "Actor1Geo_Type:Integer",
  "Actor1Geo_FullName:String",
  "Actor1Geo_CountryCode:String",
  "Actor1Geo_ADM1Code:String",
  "Actor1Geo_Lat:Float",
  "Actor1Geo_Long:Float",
  "Actor1Geo_FeatureID:Integer",
  "Actor2Geo_Type:Integer",
  "Actor2Geo_FullName:String",
  "Actor2Geo_CountryCode:String",
  "Actor2Geo_ADM1Code:String",
  "Actor2Geo_Lat:Float",
  "Actor2Geo_Long:Float",
  "Actor2Geo_FeatureID:Integer",
  "ActionGeo_Type:Integer",
  "ActionGeo_FullName:String",
  "ActionGeo_CountryCode:String",
  "ActionGeo_ADM1Code:String",
  "ActionGeo_Lat:Float",
  "ActionGeo_Long:Float",
  "ActionGeo_FeatureID:Integer",
  "DateAdded:Integer",
  "Url:String",
  "*geom:Point:srid=4326"
)

@throws(classOf[SchemaException])
def buildGDELTFeatureType(featureName:String, attributes:util.ArrayList[String]):SimpleFeatureType = {
    val name = featureName
    val spec = Joiner.on(",").join(attributes)
    val featureType = DataUtilities.createType(name, spec)
    featureType.getUserData().put(SimpleFeatureTypes.DEFAULT_DATE_KEY, "SqlDate")
    featureType
  }

val featureType:SimpleFeatureType = buildGDELTFeatureType(featureName, geoMesaSchema)

// Create the schema/feature
val ds = DataStoreFinder
    .getDataStore(dsParams)
    .asInstanceOf[AccumuloDataStore]
    .createSchema(featureType)
```

The code above is based on [this example](https://github.com/PacktPublishing/Mastering-Spark-for-Data-Science/tree/master/geomesa-utils-1.5) and [this example](http://www.geomesa.org/documentation/tutorials/geomesa-examples-gdelt.htmls).

Next we create a **Spark Session**, load the **Geomesa UDFs**, source the data and make it available as a table. Then we create a geospatial `Point` using the `st_makePoint` with the longitude and latitude values provided by the input dataset - all this with the comfort of a **SQL query**.  Finally we use the `write` together with the `save` function to store the dataset in **Accumulo**:

```scala
val spark = SparkSession
   .builder()
   .appName("Geomesa Ingest")
   .getOrCreate()

 // load Geomesa UDFs
 org.apache.spark.sql.SQLTypes.init(spark.sqlContext)

 val ingestedData = (
   spark
     .read
     .option("header", "false")
     .option("delimiter","\\t")
     .option("ignoreLeadingWhiteSpace","true")
     .option("ignoreTrailingWhiteSpace","true")
     .option("treatEmptyValuesAsNulls","true")
     .option("dateFormat","yyyyMMdd")
     .schema(gdeltSchema)
     .csv(ingestFile)
   )

 ingestedData.show(10)
 println(ingestedData.getClass)

 ingestedData.createOrReplaceTempView("ingested_data")
 // for debugging only
 // val prepedData = spark.sql("""SELECT Actor1Geo_Lat, Actor1Geo_Long, st_makePoint(Actor1Geo_Lat, Actor1Geo_Long) as geom FROM ingested_data""")
 // prepedData.show(10)
 val prepedData = spark.sql("""SELECT *, st_makePoint(Actor1Geo_Lat, Actor1Geo_Long) as geom FROM ingested_data""")

prepedData.show(10)

val prepedDataFiltered = prepedData
  .filter("geom IS NOT NULL")
  // filter out invalid lat and long
  .filter("NOT(ABS(Actor1Geo_Lat) > 90.0 OR ABS(Actor1Geo_Long) > 90.0)")

println("========= FINAL OUTPUT ===============")

prepedDataFiltered.show(10)

prepedDataFiltered
  .write
  .format("geomesa")
  .options(dsParams)
  .option("geomesa.feature","event")
  .save()
```

As you can see, using **GeoMesa Spark SQL** (as opposed to GeoMesa Spark Core) to ingest data is a piece of cake. You can find a more complete example [here](https://github.com/locationtech/geomesa/blob/master/geomesa-accumulo/geomesa-accumulo-spark-runtime/src/test/scala/org/locationtech/geomesa/accumulo/spark/AccumuloSparkProviderTest.scala#L86).

Run the code:

```bash
sbt clean assembly
# submit job
spark-submit --master local[4] \
  --class examples.IngestDataWithSpark \
  target/scala-2.11/GeoMesaSparkExample-assembly-0.1.jar
```

Let's check if the data got imported:

```
$ accumulo shell -u root -p password
$ tables -ns sparkImportTest
gdelt
gdelt_records_v2
gdelt_stats
gdelt_z2_v3
gdelt_z3_v4
$ scan -t sparkImportTest.gdelt_records_v2
```

## Errors

### Undefined function

On my first try I got the following error:

```  
Exception in thread "main" org.apache.spark.sql.AnalysisException: Undefined function: 'st_makePoint'. This function is neither a registered temporary function nor a permanent function registered in the database 'default'.; 
```
 
Jim Hughes from Locationtech explains: **GeoMesa** uses a **private** bit of the **Spark API** to add user-defined types and functions.  You'll want to make sure that the `geomesa-spark-sql_2.11` jar is on the **classpath**, and then you can call:

```
org.apache.spark.sql.SQLTypes.init(sqlContext)
```

Calling this function will add the geometric types, functions, and optimizations to the SQL Context.  As part of loading a **GeoMesa** dataset into **Spark SQL**, the code calls this function.  (This is why all these functions work when you use **GeoMesa**, etc.)

As another alternative, you can use the **GeoMesa Converter** library to load **GDELT** as a DataFrame.  You should be able to use a `spark.read("geomesa").options(params)` call to parse GDELT CSVs straight into SimpleFeatures.  That'd save needing to write SQL to munge columns into geometries, etc.

### Null geometry

```
java.lang.IllegalArgumentException: Null geometry in feature 1499498555df727e4e-a6f4-4e97-9205-aaced694be84
```

To avoid this error remove any records from your dataset that do not provide a geometry value.

### NoSuchAuthorityCodeException

```
org.opengis.referencing.NoSuchAuthorityCodeException: No code "EPSG:4326" from authority "EPSG" found for object of type "EngineeringCRS"
```

As Matthew Hallett pointed out in the Geomesa User Group, there is a missing dependency, see this [forum thread](http://osgeo-org.1560.x6.nabble.com/Facing-NoSuchAuthorityCodeException-problem-when-deployed-GeoTools-on-server-td4885362.html): "To decode a CRS code, you need access to the EPSG database, which it seems it's not present. Make sure you add gt-epsg-hsql in your project, and its dependencies, if any (you could use other EPSG factories as well, but that's the most usual)." 

Let's look on MVNRepository for the correct version: [click here](https://mvnrepository.com/artifact/org.geotools/gt-epsg-hsql).

Add this dependency to your `build.sbt`:

```
"org.geotools" % "gt-epsg-hsql" % "17.1"
```

Simply adding this dependency solves the issue.

Jim Hughes provided further info on this error:

The error is due to the lack of a GeoTools EPSG factory being available on the classpath at runtime ([More info about the EPSG options in GeoTools](http://docs.geotools.org/stable/userguide/library/referencing/index.html)).  Generally, it seems that SBT has two issues to address:  first, it doesn't necessarily pull in all the transitive dependencies list POMs and second, code loaded via SPI needs some handling to preserve entries in `META-INF/services`. 

[The SO question](https://stackoverflow.com/questions/27429097/geotools-cannot-find-hsql-epsg-db-throws-error-nosuchauthoritycodeexception) addresses the latter concern.  For the former, it may suffice to add a dependency on `gt-epsg-hsql` or `gt-epsg-wkt`.  The HSQL version of the library is preferable since it has a few more codes. 

That said, there are some caveats.  I have seen mismatches between the version of HSQL that GeoTools uses and versions available in Hadoop.  Also, HSQL sets up a temp directory in a common (yet configurable!) location.  For a system where multiple users are going to use the GeoMesa tools, some care may be required.  If those problems prove too much, one can try out the `gt-epsg-wkt` option instead.

## How to create a project based on the latest code

If the current version of **Geomesa** doesn't provide the features you require / the feature is only available in the latest code version, you can compile **Geomesa** from source. How to compile the code locally was explained in the first part of this series. 

Set up a **new project** folder and a new **sbt build file** as well as dedicated `lib` folder inside it to store the **local jar files** that you want to reference. These jar files we can copy over from the locally compiled code:

```bash
export VERSION=1.3.2-SNAPSHOT
# create a new project
mkdir geomesa-local-jar-dependencies
cd geomesa-local-jar-dependencies
mkdir lib
cd lib
export GEOMESA_GIT_HOME=/home/dsteiner/git/geomesa
cp $GEOMESA_GIT_HOME/geomesa-accumulo/geomesa-accumulo-datastore/target/geomesa-accumulo-datastore_2.11-$VERSION.jar .
cp $GEOMESA_GIT_HOME/geomesa-accumulo/geomesa-accumulo-tools/target/geomesa-accumulo-tools_2.11-$VERSION.jar .
cp $GEOMESA_GIT_HOME/geomesa-utils/target/geomesa-utils_2.11-$VERSION.jar .
cp $GEOMESA_GIT_HOME/geomesa-utils/target/geomesa-utils_2.11-$VERSION-sources.jar .
cp $GEOMESA_GIT_HOME/geomesa-spark/geomesa-spark-core/target/geomesa-spark-core_2.11-$VERSION.jar .
cp $GEOMESA_GIT_HOME/geomesa-accumulo/geomesa-accumulo-spark/target/geomesa-accumulo-spark_2.11-$VERSION.jar .

cp $GEOMESA_GIT_HOME/geomesa-accumulo/geomesa-accumulo-distributed-runtime/target/geomesa-accumulo-distributed-runtime_2.11-$VERSION.jar .

# not required as in distributed runtime
#cp ~/.ivy2/cache/org.geotools/gt-main/jars/gt-main-15.1.jar .
#cp ~/.ivy2/cache/org.geotools/gt-api/jars/gt-api-15.1.jar .
#cp ~/.ivy2/cache/com.vividsolutions/jts/jars/jts-1.13.jar .
```

The `build.sbt` file is pretty much the same as with the previous project, just that all the Locationtech references are commented out.

In **IntelliJ IDEA** right click on the `lib` folder and choose **Add as Library**. Confirm the upcoming dialog (**Level** can stay on *Project Level*).

Proceed as normal.

