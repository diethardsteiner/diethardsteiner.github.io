/**
  * Code Source: http://www.geomesa.org/documentation/tutorials/spark.html#parallel-computation-of-spatial-event-densities
  * Official Docu: http://www.geomesa.org/documentation/tutorials/spark.html#parallel-computation-of-spatial-event-densities
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
