// based on https://github.com/PacktPublishing/Mastering-Spark-for-Data-Science/tree/master/geomesa-utils-1.5
// which in turn is based on GeoMesa's MapReduce example: http://www.geomesa.org/documentation/tutorials/geomesa-examples-gdelt.html
// slightly adjusted here to work with GeoMesa 1.3.1
// test data can be found here: https://github.com/PacktPublishing/Mastering-Spark-for-Data-Science/tree/master/geomesa-utils-1.5/src/main/resources
// the test data is also available in this repo in the src/main/resource folder
package examples

import java.io.IOException
import java.text.SimpleDateFormat
import java.util
import java.util.Collections

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import com.google.common.base.Joiner
import com.google.common.collect.Lists
import com.vividsolutions.jts.geom.{Coordinate, Geometry, GeometryFactory}
import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.types._
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

  val dsParams = Map(
    "instanceId"  -> "BISSOL_CONSULTING",
    "zookeepers"  -> "127.0.0.1:2181",
    "user"        -> "root",
    "password"    -> "password",
    "tableName"   -> "sparkImportTest.gdelt"
  )


  val featureName = "event"

  // GeoMesa Feature
  var geoMesaSchema = Lists.newArrayList(
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
    "Url:String",
    "*geom:Point:srid=4326"
  )

  /**
  val featureType:SimpleFeatureType = buildGDELTFeatureType(featureName, attributes)

  // Create the schema/feature
  val ds = DataStoreFinder
      .getDataStore(dsParams)
      .asInstanceOf[AccumuloDataStore]
      .createSchema(featureType)

  val LATITUDE_COL_IDX = 39
  val LONGITUDE_COL_IDX = 40
  val DATE_COL_IDX = 1
  val ID_COL_IDX = 0
  val MINIMUM_NUM_FIELDS = 41
  var featureBuilder:SimpleFeatureBuilder = null
  val geometryFactory:GeometryFactory = JTSFactoryFinder.getGeometryFactory

  def createSimpleFeature(value:String):SimpleFeature = {
    // split the row by tab and save tokens in array
    val attributes:Array[String] = value.toString.split("\\t", -1)

    val formatter:SimpleDateFormat = new SimpleDateFormat("yyyyMMdd")

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

//    var i: Int = 0
//    while (i < attributes.length) {
//      simpleFeature.setAttribute(i, attributes(i))
//      i += 1
//    }

    // loop through values of row/array and assign to simple feature attribute
    val mySeq = 0 to attributes.length
    for(index <- mySeq){
      simpleFeature.setAttribute(index, attributes(index))
    }

    // create simple feature attribute SQLDATE by parsing original string representation of date
    simpleFeature.setAttribute("SQLDATE", formatter.parse(attributes(DATE_COL_IDX)))
    // add longitude and latitude / default geometry
    simpleFeature.setDefaultGeometry(geom)
    // return simpleFeature
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
**/
  def main(args: Array[String]) {


    //  val ingestFile = "file:///gdeltEventsTestFile.csv"
    val ingestFile = "file:///home/dsteiner/git/diethardsteiner.github.io/sample-files/gis/geomesa/src/main/resources/gdeltEventsTestFile.csv"
    //  val ingestFile = "hdfs:///user/dsteiner/gdelt-staging/gdeltEventsTestFile.csv"

//    val conf = new SparkConf()
//    val sc = new SparkContext(conf.setAppName("Geomesa Ingest"))
//
//    val distDataRDD = sc.textFile(ingestFile)
//
//
//    println("--------NO OF RECORDS: " + distDataRDD.count() + "-------------")
//    println("")
//    println("Sample of 10 lines read from source file:")
//    println("-----------------------------------------")
//    distDataRDD.take(10).foreach(println)


    val gdeltSchema = StructType(
      Array(
        StructField("GlobalEventId",IntegerType, true)
        , StructField("SqlDate",DateType, true)
        , StructField("MonthYear",IntegerType, true)
        , StructField("Year",IntegerType, true)
        , StructField("FractionDate",FloatType, true)
        , StructField("Actor1Code",StringType, true)
        , StructField("Actor1Name",StringType, true)
        , StructField("Actor1CountryCode",StringType, true)
        , StructField("Actor1KnownGroupCode",StringType, true)
        , StructField("Actor1EthnicCode",StringType, true)
        , StructField("Actor1Religion1Code",StringType, true)
        , StructField("Actor1Religion2Code",StringType, true)
        , StructField("Actor1Type1Code",StringType, true)
        , StructField("Actor1Type2Code",StringType, true)
        , StructField("Actor1Type3Code",StringType, true)
        , StructField("Actor2Code",StringType, true)
        , StructField("Actor2Name",StringType, true)
        , StructField("Actor2CountryCode",StringType, true)
        , StructField("Actor2KnownGroupCode",StringType, true)
        , StructField("Actor2EthnicCode",StringType, true)
        , StructField("Actor2Religion1Code",StringType, true)
        , StructField("Actor2Religion2Code",StringType, true)
        , StructField("Actor2Type1Code",StringType, true)
        , StructField("Actor2Type2Code",StringType, true)
        , StructField("Actor2Type3Code",StringType, true)
        , StructField("IsRootEvent",IntegerType, true)
        , StructField("EventCode",StringType, true)
        , StructField("EventBaseCode",StringType, true)
        , StructField("EventRootCode",StringType, true)
        , StructField("QuadClass",IntegerType, true)
        , StructField("GoldsteinScale",FloatType, true)
        , StructField("NumMentions",IntegerType, true)
        , StructField("NumSources",IntegerType, true)
        , StructField("NumArticles",IntegerType, true)
        , StructField("AvgTone",FloatType, true)
        , StructField("Actor1Geo_Type",IntegerType, true)
        , StructField("Actor1Geo_FullName",StringType, true)
        , StructField("Actor1Geo_CountryCode",StringType, true)
        , StructField("Actor1Geo_ADM1Code",StringType, true)
        , StructField("Actor1Geo_Lat",FloatType, true)
        , StructField("Actor1Geo_Long",FloatType, true)
        , StructField("Actor1Geo_FeatureID",StringType, true) /** used to be IntegerType but there are some string values in this column **/
        , StructField("Actor2Geo_Type",IntegerType, true)
        , StructField("Actor2Geo_FullName",StringType, true)
        , StructField("Actor2Geo_CountryCode",StringType, true)
        , StructField("Actor2Geo_ADM1Code",StringType, true)
        , StructField("Actor2Geo_Lat",FloatType, true)
        , StructField("Actor2Geo_Long",FloatType, true)
        , StructField("Actor2Geo_FeatureID",StringType, true) /** used to be IntegerType but there are some string values in this column **/
        , StructField("ActionGeo_Type",IntegerType, true)
        , StructField("ActionGeo_FullName",StringType, true)
        , StructField("ActionGeo_CountryCode",StringType, true)
        , StructField("ActionGeo_ADM1Code",StringType, true)
        , StructField("ActionGeo_Lat",FloatType, true)
        , StructField("ActionGeo_Long",FloatType, true)
        , StructField("ActionGeo_FeatureID",StringType, true) /** used to be IntegerType but there are some string values in this column **/
        , StructField("DateAdded",IntegerType, true)
        , StructField("Url",StringType, true)
      )
    )

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

    prepedData
      .write
      .format("geomesa")
      .options(dsParams)
      .option("geomesa.feature","gdelt")



    // however you can create datasets with case classes which can have custom data types, no?
    // or do we have to go all the way back to RDDs?



/**
    val processedRDD:RDD[SimpleFeature] = distDataRDD
      .mapPartitions {

        valueIterator =>

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

          valueIterator.map {
            s =>
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
    GeoMesaSpark(dsParams).save(processedRDD, dsParams, featureName)
**/

  }
}
