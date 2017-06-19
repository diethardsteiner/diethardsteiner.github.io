/**
  * Code Source: http://www.geomesa.org/documentation/user/spark/sparksql.html
  * Official Docu: http://www.geomesa.org/documentation/user/spark/sparksql.html
  *
  */

package examples

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
