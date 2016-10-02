

import java.io.FileInputStream
import java.util.Properties

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.twitter.TwitterSource
import org.apache.flink.streaming.api.TimeCharacteristic

import scala.util.parsing.json._
//import play.api.libs.json._

object FlinkTwitterStreamCount {


  def main(args: Array[String]) {

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val prop = new Properties()
    val propFilePath = "/home/dsteiner/Dropbox/development/config/twitter.properties"

      try {

        prop.load(new FileInputStream(propFilePath))
        prop.getProperty("twitter-source.consumerKey")
        prop.getProperty("twitter-source.consumerSecret")
        prop.getProperty("twitter-source.token")
        prop.getProperty("twitter-source.tokenSecret")

      } catch { case e: Exception =>
        e.printStackTrace()
        sys.exit(1)
      }

    //println(prop)

    val streamSource = env.addSource(new TwitterSource(prop))

    //streamSource.print()

    val parsedStream = streamSource.map(

      value => {

        val result = JSON.parseFull(value)

        try {
          result match {
            case Some(e:Map[String, Any]) => e
          }
        } catch { case e: Exception =>
          e.printStackTrace()
          sys.exit(1)
        }

      }
    )

    val filteredStream = parsedStream.filter( value =>  value.contains("created_at"))

    val record:DataStream[Tuple4[String, String, Double, Double]] = filteredStream.map(
      value => (
          value("lang").toString
          , value("created_at").toString
          , value("id").toString.toDouble
          , value("retweet_count").toString.toDouble
        )
    )

    val recordSlim:DataStream[Tuple2[String, Int]] = filteredStream.map(
      value => (
        value("lang").toString
        , 1
        )
    )

    /** ERROR - RESOLVE!
    val counts = recordSlim
      .keyBy(0)
      .timeWindow(Time.seconds(40))
      .sum(1)

    counts.print
      **/
    env.execute("Twitter Window Stream WordCount")
  }
}

