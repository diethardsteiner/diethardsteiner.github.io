

import java.io.FileInputStream
import java.util.Properties

import com.github.nscala_time.time.Imports._
import net.liftweb.json.JsonAST.JValue
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.twitter.TwitterSource
import net.liftweb.json._
import org.apache.flink.streaming.api.windowing.time.Time

import scala.util.parsing.json._
//import play.api.libs.json._

object FlinkTwitterStreamCount {


  def main(args: Array[String]) {

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    println("The current OS is: " + System.getProperty("os.name"))

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

    //println(prop)

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

    // structuredStream.print

    val recordSlim:DataStream[Tuple2[String, Int]] = structuredStream.map(
      value => (
        value.language
        , 1
        )
    )

    // recordSlim.print

    val counts = recordSlim
      .keyBy(0)
      .timeWindow(Time.seconds(30))
      .sum(1)

    counts.print

    env.execute("Twitter Window Stream WordCount")
  }
}

