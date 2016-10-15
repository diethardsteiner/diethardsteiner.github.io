

import java.io.FileInputStream
import java.net.{InetAddress, InetSocketAddress}
import java.util
import java.util.Properties

import com.github.nscala_time.time.Imports._
import net.liftweb.json._
import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.elasticsearch2.{ElasticsearchSink, ElasticsearchSinkFunction, RequestIndexer}
import org.apache.flink.streaming.connectors.twitter.TwitterSource
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.Requests

import scala.util.parsing.json._
//import play.api.libs.json._

object FlinkTwitterStreamCountWithEsSinkSimple {

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

    // timedStream.print()

    //load data into ElasticSearch

    //    val properties = new Properties()
    //    properties.setProperty("cluster.name", "elasticsearch")
    //    properties.setProperty("bulk.flush.max.actions", "1")

    val config = new util.HashMap[String, String]
    config.put("bulk.flush.max.actions", "1")
    config.put("cluster.name", "elasticsearch") //default cluster name: elasticsearch

    val transports = new util.ArrayList[InetSocketAddress]
    transports.add(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 9300))


    // testing simple setup
    val input:DataStream[String] = timedStream.map( _.language.toString)
    input.print()

    input.addSink(new ElasticsearchSink(config, transports, new ElasticsearchSinkFunction[String] {
      def createIndexRequest(element: String): IndexRequest = {
        val json = new util.HashMap[String, AnyRef]
        // Map stream fields to JSON properties, format:
        // json.put("json-property-name", streamField)
        json.put("data", element)
        Requests.indexRequest.index("test").`type`("test").source(json)
      }

      override def process(element: String, ctx: RuntimeContext, indexer: RequestIndexer) {
        indexer.add(createIndexRequest(element))
      }
    }))

    // before running code create index:
    // curl -XPUT 'http://localhost:9200/test'
    // curl -XGET 'http://localhost:9200/test/test/_search?pretty'
    // to remove data:
    // curl -XDELETE 'http://localhost:9200/test'


//    timedStream.addSink(new ElasticsearchSink(config, transports, new TwitterStreamInserter ))

    env.execute("Twitter Window Stream WordCount")
  }
}
