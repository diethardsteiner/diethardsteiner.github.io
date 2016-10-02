

import java.io.FileInputStream
import java.text.{ParseException, SimpleDateFormat}
import java.util.Properties

import com.github.nscala_time.time.Imports._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.twitter.TwitterSource

import scala.collection.immutable.Range
import scala.util.parsing.json._
//import play.api.libs.json._

object FlinkTwitterStreamCountWithEventTime {

  def main(args: Array[String]) {

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

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

    val streamSource = env.addSource(new TwitterSource(prop))

//    streamSource.print()

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

//    val record:DataStream[Tuple4[String, DateTime, Double, Double]] = filteredStream.map(
//      value => (
//        value("lang").toString
//        , DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss Z yyyy").parseDateTime(value("created_at").toString)
//        , value("id").toString.toDouble
//        , value("retweet_count").toString.toDouble
//        )
//    )

    case class TwitterFeed(
      language:String
      , creationTime:Long
      , id:Double
      , retweetCount:Double
    )

    val record:DataStream[TwitterFeed] = filteredStream.map(
      value => TwitterFeed(
        value("lang").toString
        , DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss Z yyyy").parseDateTime(value("created_at").toString).getMillis
        , value("id").toString.toDouble
        , value("retweet_count").toString.toDouble
        )
    )

    // https://ci.apache.org/projects/flink/flink-docs-master/dev/event_timestamp_extractors.html
    val timedStream = record.assignAscendingTimestamps(_.creationTime)

    timedStream.print()

//      val streamWithEventTime = filteredStream.assignTimestampsAndWatermarks(new AssignTimestamp())
// still in Java syntax

    /*
    [OPEN]
    - Retweet count is always 0, why?
    - id should not be a double!
    - Should created_at be interpreted as a local time instead?

     */

    env.execute("Twitter Window Stream WordCount")
  }
}

// based on http://apache-flink-mailing-list-archive.1008284.n3.nabble.com/Playing-with-EventTime-in-DataStreams-td10498.html

/** disable temporarily as otherwise code does not compile - fix later!!!
class AssignTimestamp[String] extends AscendingTimestampExtractor{
  override extractAscendingTimestamp[element:String, previousElementTimestamp:long]{

    val createDateString = element.substring(0,15) // wont work here as it is a filed in JSON
    val dateFormat:SimpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy")
    val createDate = null
    try{
      createDate = dateFormat.parse(createDateString)
    } catch(e:ParseException) {
      e.printStackTrace
    }

    createDate.getTime

  }
}
**/
