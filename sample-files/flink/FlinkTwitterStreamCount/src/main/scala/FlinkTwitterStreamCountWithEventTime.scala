

import java.io.FileInputStream
import java.text.{ParseException, SimpleDateFormat}
import java.util.Properties

import com.github.nscala_time.time.Imports._
import net.liftweb.json._
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

    val userHomeDir =
      if (System.getProperty("os.name").equals("Mac OS X"))
        "/Users/diethardsteiner/"
      else
        "/home/dsteiner/"


    val propFilePath = userHomeDir + "Dropbox/development/config/twitter.properties"

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

    //streamSource.print()

    val filteredStream = streamSource.filter( value =>  value.contains("created_at"))

    val parsedStream = filteredStream.map(
      record => {
        parse(record)
      }
    )

    //parsedStream.print()

    case class TwitterFeed(
      id:Long
      , creationTime:Long
      , language:String
      , user:String
      , favoriteCount:Int
      , retweetCount:Int
    )

    val structuredStream:DataStream[TwitterFeed] = parsedStream.map(
      record => {
        TwitterFeed(
          // ( input \ path to element \\ unboxing ) (extract no x element from list)
          ( record \ "id" \\ classOf[JInt] )(0).toLong
          , DateTimeFormat
            .forPattern("EEE MMM dd HH:mm:ss Z yyyy")
            .parseDateTime(
              ( record \ "created_at" \\ classOf[JString] )(0)
            ).getMillis
          , ( record \ "lang" \\ classOf[JString] )(0).toString
          , ( record \ "user" \ "name" \\ classOf[JString] )(0).toString
          , ( record \ "favorite_count" \\ classOf[JInt] )(0).toInt
          , ( record \ "retweet_count" \\ classOf[JInt] )(0).toInt
        )

      }
    )

    // https://ci.apache.org/projects/flink/flink-docs-master/dev/event_timestamp_extractors.html
    val timedStream = structuredStream.assignAscendingTimestamps(_.creationTime)

    timedStream.print()

//      val streamWithEventTime = filteredStream.assignTimestampsAndWatermarks(new AssignTimestamp())
// still in Java syntax

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
