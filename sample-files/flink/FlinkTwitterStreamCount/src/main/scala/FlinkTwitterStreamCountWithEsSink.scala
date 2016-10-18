

import java.io.{FileInputStream, PrintWriter, StringWriter}
import java.lang.Long
import java.net.{InetAddress, InetSocketAddress}
import java.util
import java.util.Properties

import com.github.nscala_time.time.Imports._
import net.liftweb.json._
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.connectors.twitter.TwitterSource
import org.apache.flink.streaming.connectors.elasticsearch2.{ElasticsearchSink, ElasticsearchSinkFunction, RequestIndexer}
import org.apache.flink.util.Collector
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.Requests

import scala.collection.immutable.ListMap
import scala.util.parsing.json._
//import play.api.libs.json._

object FlinkTwitterStreamCountWithEsSink {

  def main(args: Array[String]) {

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

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
      , count:Int
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
          , 1
        )

      }
    )

    // https://ci.apache.org/projects/flink/flink-docs-master/dev/event_timestamp_extractors.html
    val timedStream = structuredStream.assignAscendingTimestamps(_.creationTime)

    val config = new util.HashMap[String, String]
    config.put("bulk.flush.max.actions", "1")
    config.put("cluster.name", "elasticsearch") //default cluster name: elasticsearch

    val transports = new util.ArrayList[InetSocketAddress]
    transports.add(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 9300))

    timedStream.addSink(new ElasticsearchSink(config, transports, new ElasticsearchSinkFunction[TwitterFeed] {
      def createIndexRequest(element:TwitterFeed): IndexRequest = {
        val mapping = new util.HashMap[String, AnyRef]
        // use LinkedHashMap if for some reason you want to maintain the insert order
        // val mapping = new util.LinkedHashMap[String, AnyRef]
        // Map stream fields to JSON properties, format:
        // json.put("json-property-name", streamField)
        // the streamField type has to be converted from a Scala to a Java Type

        mapping.put("id", new java.lang.Long(element.id))
        mapping.put("creationTime", new java.lang.Long(element.creationTime))
        mapping.put("language", element.language)
        mapping.put("user", element.user)
        mapping.put("favoriteCount", new Integer((element.favoriteCount)))
        mapping.put("retweetCount", new Integer((element.retweetCount)))
        mapping.put("count", new Integer((element.count)))

        //println("loading: " + mapping)

        Requests.indexRequest.index("tweets").`type`("partition1").source(mapping)

      }

      override def process(
          element: TwitterFeed
          , ctx: RuntimeContext
          , indexer: RequestIndexer
        )
        {
          try{
            indexer.add(createIndexRequest(element))
          } catch {
            case e:Exception => println{
              println("an exception occurred: " + ExceptionUtils.getStackTrace(e))
            }
            case _:Throwable => println("Got some other kind of exception")
          }
        }
    }))

    case class TweetsByLanguage (
      language:String
      , windowStartTime:Long
      , windowEndTime:Long
      , countTweets:Int
    )

    val tweetsByLanguageStream:DataStream[TweetsByLanguage] = timedStream
      // .keyBy("language") did not work as apparently type is not picked up
      // for the key in the apply function
      // see http://stackoverflow.com/questions/36917586/cant-apply-custom-functions-to-a-windowedstream-on-flink
      .keyBy(in => in.language)
      .timeWindow(Time.seconds(30))
      .apply
      {
        (
          // tuple with key of the window
          lang: String
          // TimeWindow object which contains details of the window
          // e.g. start and end time of the window
          , window: TimeWindow
          // Iterable over all elements of the window
          , events: Iterable[TwitterFeed]
          // collect output records of the WindowFunction
          , out: Collector[TweetsByLanguage]
        ) =>
          out.collect(
            // TweetsByLanguage( lang, window.getStart, window.getEnd, events.map( _.retweetCount ).sum )
            TweetsByLanguage( lang, window.getStart, window.getEnd, events.map( _.count ).sum )
          )
      }

    // tweetsByLanguage.print

    tweetsByLanguageStream.addSink(
      new ElasticsearchSink(
        config
        , transports
        , new ElasticsearchSinkFunction[TweetsByLanguage] {

          def createIndexRequest(element:TweetsByLanguage): IndexRequest = {
            val mapping = new util.HashMap[String, AnyRef]

            mapping.put("language", element.language)
            mapping.put("windowStartTime", new Long(element.windowStartTime))
            mapping.put("windowEndTime", new Long(element.windowEndTime))
            mapping.put("countTweets", new java.lang.Integer(element.countTweets))


            // println("loading: " + mapping)
            // problem: wrong order of fields, id seems to be wrong type in general, as well as retweetCount
            Requests.indexRequest.index("tweetsbylanguage").`type`("partition1").source(mapping)

          }

          override def process(
            element: TweetsByLanguage
            , ctx: RuntimeContext
            , indexer: RequestIndexer
            )
            {
              try{
                indexer.add(createIndexRequest(element))
              } catch {
                case e:Exception => println{
                  println("an exception occurred: " + ExceptionUtils.getStackTrace(e))
                }
                case _:Throwable => println("Got some other kind of exception")
            }
          }
        }
      )
    )

    env.execute("Twitter Window Stream WordCount")
  }
}
