/**
  * Code Source: https://github.com/geomesa/geomesa-tutorials/blob/master/geomesa-examples-spark/src/main/scala/com/example/geomesa/spark/CountByDay.scala
  * Official Docu: http://www.geomesa.org/documentation/tutorials/spark.html
  *
  */

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
    // THIS EXTRACTS THE KEYS ONLY
    val rdd = spatialRDDProvider.rdd(new Configuration, sc, params, q)
    
    /** 
    rdd.take(2)
    result for `rdd` should look similar to this one:
    res0: Array[org.opengis.feature.simple.SimpleFeature] = Array(ScalaSimpleFeature:31a70f3f6e748374b22f24e0094e76b4, ScalaSimpleFeature:51117014214fa48a009c33c37e2784d7)

    you can find these keys via Accumulo as well, e.g.:
    scan -t myNamespace.gdelt_gdelt_records_v2 -r 51117014214fa48a009c33c37e2784d7
    **/

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
