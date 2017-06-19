/**
  * Code Source: https://github.com/geomesa/geomesa-tutorials/blob/master/geomesa-examples-spark/src/main/scala/com/example/geomesa/spark/ShallowJoin.scala
  * Official Docu: http://www.geomesa.org/documentation/tutorials/shallow-join.html#geomesa-spark-aggregating-and-visualizing-data
  *
  */

package examples

import com.vividsolutions.jts.geom.Geometry
import org.apache.hadoop.conf.Configuration
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.geotools.data.{DataStoreFinder, _}
import org.locationtech.geomesa.accumulo.data.AccumuloDataStore
import org.locationtech.geomesa.features.ScalaSimpleFeatureFactory
import org.locationtech.geomesa.spark.{GeoMesaSpark, GeoMesaSparkKryoRegistrator}
import org.locationtech.geomesa.utils.geotools.{SftBuilder, SimpleFeatureTypes}
import org.opengis.feature.simple.SimpleFeature

import scala.collection.JavaConversions._

object ShallowJoin {

  // set up the parameters and initialize each of the desired data stores.
  val countriesDsParams = Map(
    "instanceId" -> "BISSOL_CONSULTING",
    // zookeeper info can be found in ACCUMULO_HOME/conf/accumulo-site.xml
    "zookeepers" -> "localhost:2181",
    "user"       -> "root",
    "password"   -> "password",
    "tableName"  -> "myNamespace.countries")

  val gdeltDsParams = Map(
    "instanceId" -> "BISSOL_CONSULTING",
    // zookeeper info can be found in ACCUMULO_HOME/conf/accumulo-site.xml
    "zookeepers" -> "localhost:2181",
    "user"       -> "root",
    "password"   -> "password",
    "tableName"  -> "myNamespace.gdelt")

  val countriesDs = DataStoreFinder.getDataStore(countriesDsParams).asInstanceOf[AccumuloDataStore]
  val gdeltDs = DataStoreFinder.getDataStore(gdeltDsParams).asInstanceOf[AccumuloDataStore]

  def main(args: Array[String]) {

    // initialize a SparkContext and get the SpatialRDDProvider for each data store
    val conf = new SparkConf().setAppName("testSpark")
    val sc = SparkContext.getOrCreate(conf)

    val rddProviderCountries = GeoMesaSpark(countriesDsParams)
    val rddProviderGdelt     = GeoMesaSpark(gdeltDsParams)

    // initialize RDDs for each of the two sources
    val countriesSchema = "shapeFile"
    val gdeltSchema = "gdelt"

    val countriesRdd: RDD[SimpleFeature] = rddProviderCountries.rdd(new Configuration(), sc, countriesDsParams, new Query(countriesSchema))
    val gdeltRdd: RDD[SimpleFeature] = rddProviderGdelt.rdd(new Configuration(), sc, gdeltDsParams, new Query(gdeltSchema))

    val aggregated = shallowJoin(sc, countriesRdd, gdeltRdd, "STATE_NAME")

    //aggregated.collect.foreach {println}

    countriesDs.dispose()
    gdeltDs.dispose()

  }

  def shallowJoin(sc: SparkContext, coveringSet: RDD[SimpleFeature], data: RDD[SimpleFeature], key: String): RDD[SimpleFeature] = {
    // Broadcast sfts to executors
    GeoMesaSparkKryoRegistrator.broadcast(data)

    // Broadcast covering set to executors
    // we send our smaller data set, countries, to each of the partitions of the larger data set,
    // GDELT events. This is accomplished through a Spark broadcast, which serializes the desired
    // data and sends it to each of the nodes in the cluster.
    // Note also that we collect the countries RDD into an Array before broadcasting.
    // Spark does not allow broadcasting of RDDs, and due to the small size of the data set,
    // we can safely collect data onto the driver node without a risk of running out of memory.
    val broadcastedCover = sc.broadcast(coveringSet.collect)

    // Key data by cover name
    // With the covering set available on each partition, we can iterate over the GDELT events
    // and key them by the region they were contained in. In mapPartitions, iter is an iterator
    // to all the elements (in this case Simple Features) on the partition. Here we transform
    // each iterator and store the result into a new RDD.
    val keyedData = data.mapPartitions { iter =>
      import org.locationtech.geomesa.utils.geotools.Conversions._

      iter.flatMap { sf =>
        // Iterate over covers until a match is found
        val it = broadcastedCover.value.iterator
        var container: Option[String] = None

        while (it.hasNext) {
          val cover = it.next()
          // If the cover's polygon contains the feature,
          // or in the case of non-point geoms, if they intersect, set the container
          if (cover.geometry.intersects(sf.geometry)) {
            container = Some(cover.getAttribute(key).asInstanceOf[String])
          }
        }
        // return the found cover as the key
        if (container.isDefined) {
          Some(container.get, sf)
        } else {
          None
        }
      }
    }

    // Our new RDD is now of type RDD[(String, SimpleFeature)] and can be used for a Spark
    // reduceByKey operation, but first, we need to create a simple feature type to represent
    // the aggregated data. We first loop through the types of a sample feature from the
    // GDELT RDD to decide what fields can be aggregated.

    // Get the indices and types of the attributes that can be aggregated and send them to the partitions
    val countableTypes = Seq("Integer", "Long", "Double")
    val typeNames = data.first.getType.getTypes.toIndexedSeq.map{
      t => t.getBinding.getSimpleName.toString
    }

    val countableIndices = typeNames.indices.flatMap { index =>
      val featureType = typeNames(index)
      // Only grab countable types, skipping the ID field
      if ((countableTypes contains featureType) && index != 0) {
        Some(index, featureType)
      } else {
        None
      }
    }.toArray
    val countable = sc.broadcast(countableIndices)

    // With these fields, we can create a Simple Feature Type to store their averages and totals,
    // prefixing each one with “total_” and “avg_”. Of course, it may not make sense to aggregate
    // ID fields or fields that are already an average, should they appear, but this approach
    // makes it easy if the fields are not known ahead of time.

    // Create a Simple Feature Type based on what can be aggregated
    val sftBuilder = new SftBuilder()
    sftBuilder.stringType(key)
    sftBuilder.multiPolygon("geom")
    sftBuilder.intType("count")
    val featureProperties = data.first.getProperties.toSeq
    countableIndices.foreach{ case (index, clazz) =>
      val featureName = featureProperties.apply(index).getName
      clazz match {
        case "Integer" => sftBuilder.intType(s"total_$featureName")
        case "Long" => sftBuilder.longType(s"total_$featureName")
        case "Double" => sftBuilder.doubleType(s"total_$featureName")
      }
      sftBuilder.doubleType(s"avg_${featureProperties.apply(index).getName}")
    }
    val coverSft = SimpleFeatureTypes.createType("aggregate", sftBuilder.getSpec)

    // Register it with kryo and send it to executors
    GeoMesaSparkKryoRegistrator.register(Seq(coverSft))
    GeoMesaSparkKryoRegistrator.broadcast(keyedData)
    val coverSftBroadcast = sc.broadcast(SimpleFeatureTypes.encodeType(coverSft))

    // Pre-compute known indices and send them to workers
    val stringAttrs = coverSft.getAttributeDescriptors.map(_.getLocalName)
    val countIndex = sc.broadcast(stringAttrs.indexOf("count"))
    // Reduce features by their covering area
    val aggregate = reduceAndAggregate(keyedData, countable, countIndex, coverSftBroadcast)


    // With the totals and counts calculated, we can now compute the averages for each field. Also, while iterating,
    // we can add the country name and geometry to each feature. To do that, we first broadcast
    // a map of name to geometry.
    // Send a map of cover name -> geom to the executors
    import org.locationtech.geomesa.utils.geotools.Conversions._
    val coverMap: scala.collection.Map[String, Geometry] =
      coveringSet.map{ sf =>
        sf.getAttribute(key).asInstanceOf[String] -> sf.geometry
      }.collectAsMap

    val broadcastedCoverMap = sc.broadcast(coverMap)

    // Compute averages and set cover names and geometries
    aggregate.mapPartitions { iter =>
      import org.locationtech.geomesa.utils.geotools.Conversions.RichSimpleFeature

      iter.flatMap{ case (coverName, sf) =>
        if (sf.getType.getTypeName == "aggregate") {
          sf.getProperties.foreach{ prop =>
            val name = prop.getName.toString
            if (name.startsWith("total_")) {
              val count = sf.get[Integer]("count")
              val avg = prop.getValue match {
                case a: Integer => a.toDouble / count
                case a: java.lang.Long => a.toDouble / count
                case a: java.lang.Double => a / count
                case _ => throw new Exception(s"couldn't match $name")
              }
              sf.setAttribute(s"avg_${name.substring(6)}", avg)
            }
          }

          sf.setAttribute(key, coverName)
          sf.setDefaultGeometry(broadcastedCoverMap.value.getOrElse(coverName, null))
          Some(sf)
        } else {
          None
        }
      }
    }

    // At this point, we have created a new Simple Feature Type representing aggregated data and
    // an RDD of Simple Features of this type. The above code can all be compiled and submitted as
    // a Spark job, but if placed into a Jupyter Notebook, the RDD can be kept in memory and
    // even quickly tweaked while continuously updating visualizations.

  }

  def reduceAndAggregate(
        keyedData: RDD[(String, SimpleFeature)]
        , countable: Broadcast[Array[(Int, String)]]
        , countIndex: Broadcast[Int]
        , coverSftBroadcast: Broadcast[String]
      ): RDD[(String, SimpleFeature)] = {

    // Reduce features by their covering area
    // Now we can apply a reduceByKey operation to the keyed RDD. This Spark operation will take
    // pairs of RDD elements of the same key, apply the given function, and replace them with the
    // result. Here, we have three cases for reduction:
    // 1. The two Simple Features have not been aggregated into one of a new type.
    // 2. The two Simple Features have both been aggregated into one of a new type.
    // 3. One of the Simple Features has been aggregated (but not both).

    val aggregate = keyedData.reduceByKey((featureA, featureB) => {
      import org.locationtech.geomesa.utils.geotools.Conversions.RichSimpleFeature

      val aggregateSft = SimpleFeatureTypes.createType("aggregate", coverSftBroadcast.value)

      val typeA = featureA.getType.getTypeName
      val typeB = featureB.getType.getTypeName
      // Case: combining two aggregate features
      if (typeA == "aggregate" && typeB == "aggregate") {
        // Combine the "total" properties
        (featureA.getProperties, featureB.getProperties).zipped.foreach((propA, propB) => {
          val name = propA.getName.toString
          if (propA.getName.toString.startsWith("total_") || propA.getName.toString == "count") {
            val sum = (propA.getValue, propB.getValue) match {
              case (a: Integer, b: Integer) => a + b
              case (a: java.lang.Long, b: java.lang.Long) => a + b
              case (a: java.lang.Double, b: java.lang.Double) => a + b
              case _ => throw new Exception("Couldn't match countable type.")
            }
            featureA.setAttribute(propA.getName, sum)
          }
        })
        featureA
        // Case: combining two regular features
      } else if (typeA != "aggregate" && typeB != "aggregate") {
        // Grab each feature's properties
        val featurePropertiesA = featureA.getProperties.toSeq
        val featurePropertiesB = featureB.getProperties.toSeq
        // Create a new aggregate feature to hold the result
        val featureFields = Seq("empty", featureA.geometry) ++ Seq.fill(aggregateSft.getTypes.size - 2)("0")
        val aggregateFeature = ScalaSimpleFeatureFactory.buildFeature(aggregateSft, featureFields, featureA.getID)

        // Loop over the countable properties and sum them for both geonames simple features
        countable.value.foreach { case (index, clazz) =>
          val propA = featurePropertiesA(index)
          val propB = featurePropertiesB(index)
          val valA = if (propA == null) 0 else propA.getValue
          val valB = if (propB == null) 0 else propB.getValue

          // Set the total
          if( propA != null && propB != null) {
            val sum  = (valA, valB) match {
              case (a: Integer, b: Integer) => a + b
              case (a: java.lang.Long, b: java.lang.Long) => a + b
              case (a: java.lang.Double, b: java.lang.Double) => a + b
              case x => throw new Exception(s"Couldn't match countable type. $x")
            }
            aggregateFeature.setAttribute(s"total_${propA.getName.toString}", sum)
          } else {
            val sum = if (valA != null) valA else if (valB != null) valB else 0
            aggregateFeature.setAttribute(s"total_${propB.getName.toString}", sum)
          }
        }
        aggregateFeature.setAttribute(countIndex.value, new Integer(2))
        aggregateFeature
        // Case: combining a mix
      } else {

        // Figure out which feature is which
        val (aggFeature: SimpleFeature, geoFeature: SimpleFeature) = if (typeA == "aggregate" && typeB != "aggregate") {
          (featureA, featureB)
        } else if (typeA != "aggregate" && typeB == "aggregate") {
          (featureB, featureA)
        }

        // Loop over the aggregate feature's properties, adding on the regular feature's properties
        aggFeature.getProperties.foreach { prop =>
          val name = prop.getName.toString
          if (name.startsWith("total_")) {
            val geoProp = geoFeature.getProperty(name.substring(6))
            if (geoProp != null) {
              val sum = (prop.getValue, geoProp.getValue) match {
                case (a: Integer, b: Integer) => a + b
                case (a: java.lang.Long, b: java.lang.Long) => a + b
                case (a: java.lang.Double, b: java.lang.Double) => a + b
                case _ => 0
              }
              aggFeature.setAttribute(name, sum)
            }
          }

        }
        aggFeature.setAttribute(countIndex.value, aggFeature.get[Integer](countIndex.value) + 1)
        aggFeature
      }
    })
    aggregate
  }

}