---
layout: post
title: "Big Data Geospatial Analysis with Apache Spark, GeoMesa and Accumulo - Part 4: Additional Examples"
summary: This article walks you through practical GeoMesa examples.
date: 2017-07-01
categories: Geospatial
tags: Geospatial, Spark
published: false
---  

## Ingesting Data with Spark

I couldn't get this example working with Geomesa v1.3.1, so I tried to get it going with a locally compile version of 1.3.2 (the latest and greatest at the time of this writing). How to compile the code locally was explained in the first part of the series. I set up a new project folder and a new sbt build file as well as dedicated `lib` folder inside it to store the local jar files that I wanted to reference. These jar files we copy over from the locally compiled code:

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

The `build.sbt` file is pretty much the same as with the previous project, just that all the locationtech references are commented out.

In **IntelliJ IDEA** right click on the `lib` folder and choose **Add as Library**. Confirm the upcoming dialog (**Level** can stay on *Project Level*).

The code in this section is mainly based on code provided with the [Mastering Spark for Data Science](https://github.com/PacktPublishing/Mastering-Spark-for-Data-Science/tree/master/geomesa-utils-1.5) book. I adjusted the code to make it work with GeoMesa v1.3.1.

Add this dependency to your `build.sbt`:

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

Next upload the GeoMesa dependencies to HDFS (adjust to your version and setup):

```bash
export VERSION=1.3.2-SNAPSHOT
#export VERSION=1.3.1
cd $GEOMESA_ACCUMULO_HOME
hdfs dfs -mkdir -p /accumulo/classpath/sparkImportTest
hdfs dfs -copyFromLocal dist/accumulo/geomesa-accumulo-distributed-runtime_2.11-$VERSION.jar /accumulo/classpath/sparkImportTest 
hdfs dfs -ls /accumulo/classpath/sparkImportTest
```

Upload the file to ingest to HDFS (although this is not really necessary):

```bash
hdfs dfs -mkdir -p /user/dsteiner/gdelt-staging
hdfs dfs -copyFromLocal src/main/resources/gdeltEventsTestFile.csv /user/dsteiner/gdelt-staging
hdfs dfs -ls /user/dsteiner/gdelt-staging
```

Restart **Accumulo**.

The code to ingest the file looks like this:

```scala
import com.vividsolutions.jts.geom.{Coordinate, Geometry, GeometryFactory}

import org.geotools.data.{DataStoreFinder, DataUtilities}
import org.geotools.feature.SchemaException
import org.geotools.feature.simple.SimpleFeatureBuilder
import org.geotools.geometry.jts.JTSFactoryFinder

import org.locationtech.geomesa.accumulo.data.AccumuloDataStore
//import org.locationtech.geomesa.accumulo.index.Constants
import org.locationtech.geomesa.utils.interop.SimpleFeatureTypes
import org.locationtech.geomesa.spark.GeoMesaSpark
import org.locationtech.geomesa.spark.api.java.JavaSpatialRDDProvider

import org.opengis.feature.simple.{SimpleFeature, SimpleFeatureType}

import scala.collection.JavaConversions._

object IngestDataWithSpark {

  val dsConf = Map(
    "instanceId" -> "BISSOL_CONSULTING",
    "zookeepers" -> "127.0.0.1:2181",
    "user" -> "root",
    "password" -> "password",
    "tableName" -> "sparkImportTest.gdelt")

  var LATITUDE_COL_IDX = 39
  var LONGITUDE_COL_IDX = 40
  var DATE_COL_IDX = 1
  var ID_COL_IDX = 0
  var MINIMUM_NUM_FIELDS = 41
  var featureBuilder: SimpleFeatureBuilder = null
  var geometryFactory: GeometryFactory = JTSFactoryFinder.getGeometryFactory

  val featureName = "event"
//  val ingestFile = "file:///gdeltEventsTestFile.csv"
  val ingestFile = "hdfs:/user/dsteiner/gdelt-staging/gdeltEventsTestFile.csv"
  var attributes = Lists.newArrayList(
    "GLOBALEVENTID:Integer",
    "SQLDATE:Date",
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
    "DATEADDED:Integer",
    "*geom:Point:srid=4326")

  val featureType:SimpleFeatureType = buildGDELTFeatureType(featureName, attributes)
  // create the schema/feature
  val ds = DataStoreFinder
      .getDataStore(dsConf)
      .asInstanceOf[AccumuloDataStore]
      .createSchema(featureType)

  def createSimpleFeature(value:String):SimpleFeature = {

    val attributes: Array[String] = value.toString.split("\\t", -1)
    val formatter: SimpleDateFormat = new SimpleDateFormat("yyyyMMdd")

    featureBuilder.reset
    val lat:Double = attributes(LATITUDE_COL_IDX).toDouble
    val lon:Double = attributes(LONGITUDE_COL_IDX).toDouble
    if (Math.abs(lat) > 90.0 || Math.abs(lon) > 180.0) {
      // log invalid lat/lon
    }

    val geom:Geometry =
        geometryFactory
          .createPoint(
            new Coordinate(lon, lat)
          )

    val simpleFeature:SimpleFeature =
        featureBuilder
          .buildFeature(
            attributes(ID_COL_IDX)
          )

    var i: Int = 0
    while (i < attributes.length) {
      simpleFeature.setAttribute(i, attributes(i))
      i += 1
    }
    simpleFeature.setAttribute("SQLDATE", formatter.parse(attributes(DATE_COL_IDX)))
    simpleFeature.setDefaultGeometry(geom)
    simpleFeature
  }
  // this section is based on 1.2.1, see: http://www.geomesa.org/documentation/1.2.1/tutorials/geomesa-examples-gdelt.html
  // same example available for 1.3.1, see: http://www.geomesa.org/documentation/tutorials/geomesa-examples-gdelt.html
  // code available: geomesa-examples-gdelt/src/main/java/com/example/geomesa/gdelt/GDELTIngest.java
  @throws(classOf[SchemaException])
  def buildGDELTFeatureType(featureName:String, attributes:util.ArrayList[String]):SimpleFeatureType = {
    val name = featureName
    val spec = Joiner.on(",").join(attributes)
    val featureType = DataUtilities.createType(name, spec)
    // featureType.getUserData.put(Constants.SF_PROPERTY_START_TIME, "SQLDATE")
    featureType.getUserData().put(SimpleFeatureTypes.DEFAULT_DATE_KEY, "SQLDATE");
    featureType
  }

  def main(args: Array[String]) {

    val conf = new SparkConf()
    val sc = new SparkContext(conf.setAppName("Geomesa Ingest"))

    val distDataRDD = sc.textFile(ingestFile)

    val processedRDD:RDD[SimpleFeature] = distDataRDD.mapPartitions { valueIterator =>

      if (valueIterator.isEmpty) {
        Collections.emptyIterator
      }

      //  setup code for SimpleFeatureBuilder
      try {
        val featureType: SimpleFeatureType = buildGDELTFeatureType(featureName, attributes)
        featureBuilder = new SimpleFeatureBuilder(featureType)
      }
      catch {
        case e: Exception => {
          throw new IOException("Error setting up feature type", e)
        }
      }

      valueIterator.map { s =>
        // Processing as before to build the SimpleFeatureType
        val simpleFeature = createSimpleFeature(s)
        if (!valueIterator.hasNext) {
          // cleanup here
        }
        simpleFeature
      }
    }


    // New save method explained on:
    // http://www.geomesa.org/documentation/user/spark/core.html
    GeoMesaSpark(dsConf).save(processedRDD, dsConf, featureName)


  }
}
```

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

```

### Errors

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

[The SO question](https://stackoverflow.com/questions/27429097/geotools-cannot-find-hsql-epsg-db-throws-error-nosuchauthoritycodeexception) address the latter concern.  For the former, it may suffice to add a dependency on gt-epsg-hsql or gt-epsg-wkt.  The HSQL version of the library is preferable since it has a few more codes. 

That said, there are some caveats.  I have seen mismatches between the version of HSQL that GeoTools uses and versions available in Hadoop.  Also, HSQL sets up a temp directory in a common (yet configurable!) location.  For a system where multiple users are going to use the GeoMesa tools, some care may be required.  If those problems prove too much, one can try out the `gt-epsg-wkt` option instead.

## GeoServer Integration

The [GeoMesa and GeoServer](http://www.geomesa.org/documentation/user/architecture.html#geomesa-and-geoserver) article provides some interesting background info.

### Connecting to Accumulo

[Visualize Data With GeoServer](http://www.geomesa.org/documentation/tutorials/geomesa-quickstart-accumulo.html#visualize-data-with-geoserver)

1. Log on to the **GeoServer** web page using the default user `admin` and password `geoserver`. 
2. From the left hand side panel chose **Stores**. 
3. On the **New data source** page under **Vector Data Sources** choose **Accumulo (GeoMesa)**. (This option will only show up if you installed the GeoMesa plugin correctly.)
4. On the **New Vector Data Source** define the connection details.

### Example

The [GeoMesa Accumulo Quick Start](http://www.geomesa.org/documentation/tutorials/geomesa-quickstart-accumulo.html#geomesa-accumulo-quick-start) is a good starting point.

The docu example [Web Processing Services (WPS) Tube Select](http://www.geomesa.org/documentation/tutorials/geomesa-tubeselect.html) looks at time-interpolated (both location and time change) queries using a Twitter data. If you are interested, explore this example by yourself.

## Jupyter integration

... this section is still developing ...

- [Deploying GeoMesa Spark with Jupyter Notebook](http://www.geomesa.org/documentation/user/spark/jupyter.html)
- [GeoMesa Spark: Aggregating and Visualizing Data](http://www.geomesa.org/documentation/tutorials/shallow-join.html#geomesa-spark-aggregating-and-visualizing-data)

### Leaflet

See [Sample Notebook - Leaflet](https://github.com/locationtech/geomesa/tree/master/geomesa-jupyter/geomesa-jupyter-leaflet).

### Vegas 

Copied from [Source](http://www.geomesa.org/documentation/user/spark/jupyter.html#configure-toree-and-geomesa)

To use Vegas within Jupyter, load the appropriate libraries and a displayer:

```scala
import vegas._
import vegas.render.HTMLRenderer._
import vegas.sparkExt._

implicit val displayer: String => Unit = { s => kernel.display.content("text/html", s) }
```

Then use the `withDataFrame` method to plot data in a `DataFrame`:

```scala
Vegas("Simple bar chart").
  withDataFrame(df).
  encodeX("a", Ordinal).
  encodeY("b", Quantitative).
  mark(Bar).
  show(displayer)
```

### Example

Based on [GeoMesa Spark: Aggregating and Visualizing Data](http://www.geomesa.org/documentation/tutorials/shallow-join.html#geomesa-spark-aggregating-and-visualizing-data).

> **Important**: When I first worked through this tutorial in May 2017 the shapefile importer had a bug. Emilio nearly instantly fixed this and I compiled the master branch directly from source. You might have to do the same. [Check this](https://github.com/locationtech/geomesa/pull/1512).

Again, we use the GDELT event data. In addition, we also need a shapefile of **polygons** outlining your regions of choice. You can download one from [Thematicmapping.org](http://thematicmapping.org/downloads/world_borders.php): From the **Download** section choose `TM_WORLD_BORDERS_SIMPL-0.3.zip`. Or alternatively run:

```bash
cd ~/Downloads
wget http://thematicmapping.org/downloads/TM_WORLD_BORDERS_SIMPL-0.3.zip
unzip TM_WORLD_BORDERS_SIMPL-0.3.zip
```

#### Creating a Feature

> **Note**: It is not necessary to create a schema/feature. Just run the ingest command without specifying a schema and GeoMesa will create one for you. However, for a full learning experience, we will have a look at how to create a schema from scratch, even though the one shown below is not a 100% correct. I suggest you just read through this section and execute what is listed in the next section (the ingest command without the schema reference).

So how do we ingest a shapefile, you might ask? Again, [the official docu](http://www.geomesa.org/documentation/1.3.0/user/accumulo/commandline_tools.html?highlight=shapefile) helps us out. But first we have to create a feature/schema: The shapefile came with a README file, which describes the data structure:

COLUMN    | TYPE | DESCRIPTION
----------|------|------------
Shape     | Polygon | Country/area border as polygon(s)
FIPS      | String(2) | FIPS 10-4 Country Code
ISO2      | String(2) | ISO 3166-1 Alpha-2 Country Code
ISO3      | String(3) | ISO 3166-1 Alpha-3 Country Code
UN        | Short Integer(3) | ISO 3166-1 Numeric-3 Country Code 
NAME      | String(50) | Name of country/area
AREA      | Long Integer(7) | Land area, FAO Statistics (2002) 
POP2005   | Double(10,0) | Population, World Population Prospects (2005)
REGION    | Short Integer(3) | Macro geographical (continental region), UN Statistics
SUBREGION | Short Integer(3) | Geogrpahical sub-region, UN Statistics
LON       | FLOAT (7,3) | Longitude
LAT       | FLOAT (6,3) |Latitude

Which data types are available for features?

Take a look at the `sfts` (**Simple Feature Type Specification**) in `$GEOMESA_ACCUMULO_HOME/conf/application.conf` to find some of the common types:

- `String`
- `Integer`
- `Long`
- `Double`
- `Date`
- `List[String]`
- `Map[String,Int]`
- `Point`
- `LineString`
- `Polygon`

[GeoTools](http://www.geotools.org/) uses a `SimpleFeatureType` to represent the schema for individual `SimpleFeatures`. We can easily create a schema using the [GeoTools DataUtilities class](http://docs.geotools.org/latest/userguide/library/main/feature.html). The schema string is a **comma separated list of attribute descriptors** of the form `:`, e.g. `Year:Integer`. Some attributes may have a third term with an appended **hint**, e.g. `geom:Point:srid=4236`, and the default geometry attribute is often prepended with an asterisk. For example, a complete schema string for a `SimpleFeatureType` describing a city with a latitude/longitude point, a name, and a population might be `*geom:Point:srid=4326,cityname:String,population:Integer`. [Source](http://www.geomesa.org/documentation/tutorials/geomesa-examples-gdelt.html). `srid=4326` is the coordinate system.

> **Note**: Not every word is accepted, take a look at this [list reserved GeoMesa words](http://www.geomesa.org/documentation/user/datastores/reserved_words.html#reserved-words).

```
# create feature/schema
geomesa create-schema \
    -u root \
    -c myNamespace.countries \
    -f countriesFeature \
    -s shape:Polygon,fips:String,iso2:String,iso3:String,un:Integer,name:String,area:String,pop2005:Double,region:Integer,subregion:Integer,lon:Float,lat:Float
 
# ingest data
geomesa ingest -u root -p password \
  -c myNamespace.countries -f shapeFile countriesFeature TM_WORLD_BORDERS_SIMPL-0.3.shp
```

#### Errors when ingesting Shapefile

In example I got an index out of bound exception:

```
ERROR requirement failed: Values out of bounds ([-180.0 180.0] [-90.0 90.0]): [-180.0 180.00000190734863] [-90.0 -60.54722595214844]
java.lang.IllegalArgumentException: requirement failed: Values out of bounds ([-180.0 180.0] [-90.0 90.0]): [-180.0 180.00000190734863] [-90.0 -60.54722595214844]
    at scala.Predef$.require(Predef.scala:219)
    at org.locationtech.geomesa.curve.XZ2SFC.org$locationtech$geomesa$curve$XZ2SFC$$normalize(XZ2SFC.scala:321)
    at org.locationtech.geomesa.curve.XZ2SFC.index(XZ2SFC.scala:55)
    at org.locationtech.geomesa.index.index.XZ2Index$$anonfun$toIndexKey$1.apply(XZ2Index.scala:124)
```

This happens in **GeoMesa version 1.3.1**.

Emilio and Jim kindly clarified: 

"Likely the schema you're creating doesn't quite align with the shapefile. Try the ingest command without creating the schema first - the ingest command will create it for you. You can either delete the existing schema or change the catalog table.

We use the geotools shapefile data store to read a shapefile - I believe that you pass it the primary .shp file, but it expects the other files to be alongside.

We have assumptions that data is in EPSG:4326 (longitude, latitude) and the points are inside that CRS's bounding box.  


If the shapefile violates either of those conditions, it might need a little pre-processing TLC.  

Generally, with those assumptions, as Emilio said, you shouldn't need to create the schema beforehand."

As mentioned above, use a version later than 1.3.1, which will have a bug fix for this problem.

Command to import without specifying schema:

I quickly tried without specifying the feature/schema:

```
geomesa ingest -u root -p password \
  -c myNamespace.countries -f shapeFile TM_WORLD_BORDERS_SIMPL-0.3.shp
```

We can check the schema that was automatically generated for us:

```bash
$ geomesa get-type-names -u root  -c myNamespace.countries
Password (mask enabled)> 
Current features types:
shapeFile
$ geomesa describe-schema -u root  -c myNamespace.countries -f shapeFile
Password (mask enabled)> 
INFO  Describing attributes of feature 'shapeFile'
the_geom  | MultiPolygon (Spatially indexed)
FIPS      | String       
ISO2      | String       
ISO3      | String       
UN        | Integer      
NAME      | String       
AREA      | Integer      
POP2005   | Long         
REGION    | Integer      
SUBREGION | Integer      
LON       | Double       
LAT       | Double       

User data:
  geomesa.indices              | xz2:1:3,records:2:3
  geomesa.table.sharing        | true
```

#### The Code

If you don't know the name of the associated schema, you can e.g. run the following command:

```bash
$ geomesa get-type-names -u root -c myNamespace.countries
Current features types:
shapeFile
```

Here we try to find out the schema name for the countries data we uploaded earlier on.


#### Run Job

Build fat jar as shown in the previous examples. Make sure Spark is running.

Run job:

```bash
# submit job
spark-submit --master local[4] \
  --class examples.ShallowJoin \
  target/scala-2.11/GeoMesaSparkExample-assembly-0.1.jar
```

#### Visualising

In **Jupyter** create a new **notebook**.

The code from the previous section has to be adjusted a bit to work within the notebook. I leave this exercise to you.

##### Add GeoTools GeoJSON dependency

We can export our RDD as **GeoJSON** using **Toree's** `AddDeps` function:

```
%AddDeps org.geotools gt-geojson 14.1 --transitive --repository http://download.osgeo.org/webdav/geotools
```

##### Transform Simple Features to GeoJSON

Now we can then transform the RDD of Simple Features to an RDD of strings, collect those strings from each partition, join them, and write them to a file:

```scala
import org.geotools.geojson.feature.FeatureJSON
import java.io.StringWriter
val geoJsonWriters = averaged.mapPartitions{ iter =>
    val featureJson = new FeatureJSON()
    val strRep = iter.map{ sf =>
        featureJson.toString(sf)
    }
    // Join all the features on this partition
    Iterator(strRep.mkString(","))
}
// Collect these strings and joing them into a JSON array
val geoJsonString = geoJsonWriters.collect.mkString("[",",","]")
```

##### Write GeoJSON to File

```scala
import java.io.File
import java.io.FileWriter
val jsonFile = new File("aggregateGdeltEarthJuly.json")
val fw = new FileWriter(jsonFile)
fw.write(geoJsonString)
fw.close
```

##### Add Leaflet Styles and Javascript

For the visualisation add a new paragraph and choose the `%%HTML` interpreter to reference the **Leaflet** JavaScript library:

```html
%%HTML
<link rel="stylesheet" href="http://cdn.leafletjs.com/leaflet/v0.7.7/leaflet.css" />
<script src="http://cdn.leafletjs.com/leaflet/v0.7.7/leaflet.js"></script>
<style>
.info { padding: 6px 8px; font: 14px/18px Arial, Helvetica, sans-serif; background: white; background: rgba(255,255,255,0.8); box-shadow: 0 0 15px rgba(0,0,0,0.2); border-radius: 5px; } 
.info b { margin: 0 0 5px; color: #777; }
.legend {
    line-height: 18px;
    color: #555;
}
.legend i {
    width: 18px;
    height: 18px;
    float: left;    
    opacity: 0.7;
}</style>
```

In order to modify the DOM of the HTML document from within a Jupyter cell, we must set up a **Mutation Observer** to correctly **respond to asynchronous changes**. We attach the observer to `element`, which refers to the cell from which the JavaScript code is run. Within this observer, we instantiate a new **Leaflet map**, and add a base layer from **OSM**.

Inside the Leaflet we create a **tile layer** from the **GeoJSON** file we created. There are further options of creating a layer from an image file or from a GeoServer WMS layer.

Next we color each country’s polygon by its average [Goldstein scale](http://web.pdx.edu/~kinsella/jgscale.html), indicating how events are contributing to the stability of a country during that time range.

The **Goldstein Scale** is a metric of how events contribute to the stability of a country. 

```javascript
%%javascript

(new MutationObserver(function() {
    // START - leaflet
    
    // Add the base map and center around US
    var map = L.map('map').setView([35.4746,-44.7022],3);
    L.tileLayer("http://{s}.tile.osm.org/{z}/{x}/{y}.png").addTo(map); 
    
    // Function to set popups for each feature
    function onEachFeature(feature, layer) {
        layer.bindPopup(feature.properties.popupContent);        
    }

    // Colors for population levels
    var colorRange = ["#d73027","#f46d43","#fdae61","#fee08b","#ffffbf","#d9ef8b","#a6d96a","#66bd63","#1a9850"];
    var grades = [-3, -2.25, -1.5, -0.75, 0, 0.75, 1.5, 2.25, 3];
    // Function to set popup content and fill color 
    function decorate(feature) {

        // Set the popup content to be the country's properties
        var popup = "";
        for (var prop in feature.properties) {
            popup += (prop + ": " + feature.properties[prop] + "<br/>")            
        }
        feature.properties.popupContent = popup;    

        // Set fill color based on goldstein scale
        var fillColor = colorRange[8];        
        for (var x = 0; x < 9; x++) {
            if (feature.properties.avg_goldsteinScale < grades[x]) {
                fillColor = colorRange[x]
                break
            }
        }            

        feature.properties.style = {
            color: "black",
            opacity: ".6",
            fillColor: fillColor,
            weight: ".5",
            fillOpacity: ".6"
        }        
    }

    // Create the map legend
    var legend = L.control({position: "bottomright"});

    legend.onAdd = function (map) {

        var div = L.DomUtil.create("div", "info legend");

        div.innerHTML+="<span>Avg. Goldstein Scale</span><br/>";
        // create a color tile for each interval
        for (var i = 0; i < grades.length; i++) {
            div.innerHTML +=
                "<i style='background:" + colorRange[i] + "'></i> ";
        }
        div.innerHTML += "<br/>";
        
        // label bounds of intervals
        div.innerHTML += "<i>"+grades[0]+"</i>";
        for (var i = 1; i < grades.length-1; i++) {
            div.innerHTML +="<i></i>"
        }
        div.innerHTML += "<i>"+grades[8]+"</i>";

        return div;
    };

    legend.addTo(map);


    var info = L.control();

    info.onAdd = function (map) {
        this._div = L.DomUtil.create("div", "info");
        this.update();
        return this._div;
    };

    info.update = function (props) {
        this._div.innerHTML = "<b>GDELT Data by Country</b>"
    };

    info.addTo(map);
    // Open the geojson file and add it as a layer
    var rawFile = new XMLHttpRequest();
        rawFile.onreadystatechange = function () {                
        if(rawFile.readyState === 4) {                                   
            if(rawFile.status === 200 || rawFile.status == 0) {                
                var allText = rawFile.response;
                var gdeltJson = JSON.parse(allText)    
                console.log(gdeltJson)
                gdeltJson.forEach(decorate)
                L.geoJson(gdeltJson, {
                    style: function(feature) { return feature.properties.style},
                    onEachFeature: onEachFeature
                }).addTo(map); 
                // Css override
                $('svg').css("max-width","none")
            }
        }
    }        
    rawFile.open("GET", "aggregateGdeltEarthJuly.json", false);
    rawFile.send()

    //END - leaflet
    this.disconnect()
})).observe(element[0], {childList: true})


element.append($('<div/>', { id: "map", width: "100%", height: "512px" }))
```


### Zeppelin Integration

[Deploying GeoMesa Spark with Zeppelin](http://www.geomesa.org/documentation/current/user/spark/zeppelin.html)

