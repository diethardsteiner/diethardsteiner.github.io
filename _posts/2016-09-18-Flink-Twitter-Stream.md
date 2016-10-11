---
layout: post
title: "Apache Flink Streaming: Simple Twitter Example"
summary: This article provides a short intro into the fascinating world of Apache Flink
date: 2016-09-18
categories: Flink
tags: Flink
published: false
--- 

## Creating SBT build file referencing latest Snapshot

Some features are only available in the very latest code base (like the **Twitter Connector** we will be working with today), hence I will briefly talk you through how to set up your SBT project to download the dependencies from the **snapshot repository**. For a detailed Flink SBT project setup guide please read my previous blog post [Getting Started with Apache Flink](REF MISSING) - here I will only focus on the creating the `build.sbt` file.

As snapshots are not available by the standard repositories, we have to explicitly define it via a [Resolver](http://www.scala-sbt.org/0.13/docs/Resolvers.html) in our **SBT** build file:

```
resolvers += "apache.snapshots" at "https://repository.apache.org/content/repositories/snapshots/"
```


You can just [browse the repo](https://repository.apache.org/content/repositories/snapshots/org/apache/flink/) via your web browser as well to find the correct version.

Some sources show how to define the **Maven** dependencies, e.g.:


```
<dependency>
	<groupId>org.apache.flink</groupId>
	<artifactId>flink-streaming-scala_2.10</artifactId>
	<version>1.2-SNAPSHOT</version>
</dependency>
```

You might make the mistake to copy the `artifactId` value to your **SBT** build file like so:

```
libraryDependencies ++= Seq(
  "org.apache.flink" %% "flink-streaming-scala_2.10"  % "1.2-SNAPSHOT"
)
```

Once you compile this with SBT you will get the following warning follwed by an error:

```
[warn]   https://repository.apache.org/content/repositories/snapshots/org/apache/flink/flink-clients_2.10_2.10/1.2-SNAPSHOT/flink-clients_2.10_2.10-1.2-SNAPSHOT.pom
...
```

As you can see `_2.10` is now mentioned twice, which is clearly not right! So make sure you omit it in the `artifactId` for your **SBT** build file as the **Scala version** (in my case 2.10) is mentioned in `scalaVersion` any ways already! 

The correct definition looks like this:

```properties
name := "FlinkTwitterStreamCount"

version := "1.0"

scalaVersion := "2.10.4"

organization := "com.bissolconsulting"

resolvers += "apache.snapshots" at "https://repository.apache.org/content/repositories/snapshots/"

val flinkVersion = "1.2-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.apache.flink" %% "flink-streaming-scala"  % flinkVersion,
  "org.apache.flink" %% "flink-clients"          % flinkVersion
)
```

In your project directory run `sbt` now and issue a `compile`. Once it compiles successfully, let's add the **Twitter Connector Dependency**:

In the [official docu](https://ci.apache.org/projects/flink/flink-docs-master/dev/connectors/twitter.html) we can find the Maven dependency definition:

```xml
<dependency>
  <groupId>org.apache.flink</groupId>
  <artifactId>flink-connector-twitter_2.10</artifactId>
  <version>1.2-SNAPSHOT</version>
</dependency>
```

This translates into **SBT** like so (for clarity I show the whole `build.sbt` file again):

```
name := "FlinkTwitterStreamCount"

version := "1.0"

scalaVersion := "2.10.4"

organization := "com.bissolconsulting"

resolvers += "apache.snapshots" at "https://repository.apache.org/content/repositories/snapshots/"

val flinkVersion = "1.2-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.apache.flink" %% "flink-streaming-scala"  % flinkVersion,
  "org.apache.flink" %% "flink-clients"          % flinkVersion,
  "org.apache.flink" %% "flink-connector-twitter"          % flinkVersion
)
```

Still in the **SBT interactive shell** type `reload` to reload the build configuration and then issue a `clean` followed by a `compile`.

## Coding in IntelliJ IDEA

Since we appreciate the convenience that an IDE provides, we will add our **SBT** project as a new project to **IDEA**.

### Creating the Twitter Source

Create the `FlinkTwitterStreamCount.scala` file with following content:

```scala
import java.util.Properties
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.twitter.TwitterSource

object FlinkTwitterStreamCount {
  def main(args: Array[String]) {

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val props = new Properties()
    props.setProperty(TwitterSource.CONSUMER_KEY, "<your-value>")
    props.setProperty(TwitterSource.CONSUMER_SECRET, "<your-value>")
    props.setProperty(TwitterSource.TOKEN, "<your-value>")
    props.setProperty(TwitterSource.TOKEN_SECRET, <your-value>")
    val streamSource = env.addSource(new TwitterSource(props))
    
    streamSource.print()
    
    env.execute("Twitter Window Stream WordCount")
  }
}
```

> **Note**: Do not leave your connection details directly in the code! We will later on look at a way to externalise them.

We setup a `TwitterSource` reader proving all the authentication details (as described in [the official docu](https://ci.apache.org/projects/flink/flink-docs-master/dev/connectors/twitter.html)). Compile and execute our program by clicking onto the **Run** button. This emits a string with random Twitter records in **JSON** format, e.g.:

```javascript
{
  "delete": {
    "status": {
      "id": 715433581762359296,
      "id_str": "715433581762359296",
      "user_id": 362889585,
      "user_id_str": "362889585"
    },
    "timestamp_ms": "1474193375156"
  }
}
```

Note there is an [official Flink Twitter Java example](https://github.com/apache/flink/blob/9f52422350e145454d4b6972741944b1fd2185f2/flink-examples/flink-examples-streaming/src/main/java/org/apache/flink/streaming/examples/twitter/TwitterExample.java) available which is work checking out!

### Externalising the Twitter Connection Details

Best practices first (oh well, second in the case)! We will store the **Twitter token** details in a **properties file** called `twitter.properties`:

```
twitter-source.consumerKey=<your-details>
twitter-source.consumerSecret=<your-details>
twitter-source.token=<your-details>
twitter-source.tokenSecret=<your-details>
```

We will add following code to our **Scala** file:

```scala
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
```
 
This code requires the `scala.util.parsing.json` library, so add this one to the import statements as well.
 
 
 Ok, that's this done then. We can no progres with a good conscience ;)

### Handling the returned JSON stream

The next task is to convert the stringified JSON to a proper Scala representation: As suggested by [this Stackoverflow](http://stackoverflow.com/questions/30884841/converting-json-string-to-a-json-object-in-scala) answer we will could make use of the [ScalaJson](https://www.playframework.com/documentation/2.5.x/ScalaJson) library from the **Play Framework** to do this and add this dependency to our `build.sbt` file:

```
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.4.8"
```

Link to [repo](https://mvnrepository.com/artifact/com.typesafe.play/play-json_2.10/2.4.8). However, we can just use the built in Scala library for now as well.

We will use the native **Scala JSON parser** (`scala.util.parsing.json._`) to turn the String into a proper collection. For now we will simply use a map function, check that the parsing is successful (using a case statement??) and then simply print out the result:

```scala
    streamSource.map( value => {

        val result = JSON.parseFull(value)

        result match {
          case Some(e:Map[String, Any]) => println(e)   // println(e("retweeted"))
          case None => println("Error")
        }

      }
    )
```

### Creating a Tuple

The next step is check filter out some records and convert the `Map` to `Tuple`, allowing us to keep certain fields only:

```scala
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
```

### Creating a Tumbling Window Aggregation

While this is an interesting exercise, we cannot really send the above tuple like this to the **windowed aggregate function**: The `created_at` is too granular, the same goes for the `id` (quite likely), so any **sum** e.g. that we perform on `retweet_count` will be returned on the source data row level. We will prepare a super simple `DataStream` now which only contains the language field and a constant counter. This should answer the question: How many tweets are published in a given language in 40 seconds intervals:

```scala
    val recordSlim:DataStream[Tuple2[String, Int]] = filteredStream.map(
      value => (
        value("lang").toString
        , 1
        )
    )

    val counts = recordSlim
      .keyBy(0)
      .timeWindow(Time.seconds(40))
      .sum(1)

    counts.print
```

While simple, this example is far from ideal. We should at least use the `created_at` time as the event time ...

## Converting String to Date

The field `created_at` is currently of type `String`. We will make use of the [nscala-time library](https://github.com/nscala-time/nscala-time) which is mainly a wrapper around **Joda time**. Add to your **SBT build** file the following dependency:

```
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.14.0"
```

And to your **Scala** file the following import statement:

```scala
import com.github.nscala_time.time.Imports._
```

We will also have to apply a **formatting mask** and if we need some help on this, we can take a look at the [Joda Format Documentation](http://www.joda.org/joda-time/key_format.html). The `create_at` value looks something like this: `Sun Sep 18 10:09:34 +0000 2016`, so the corresponding formatting mask is `EE MMM dd HH:mm:ss Z yyyy`:

```scala
    val record:DataStream[Tuple4[String, DateTime, Double, Double]] = filteredStream.map(
      value => (
        value("lang").toString
        , DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss Z yyyy").parseDateTime(value("created_at").toString)
        , value("id").toString.toDouble
        , value("retweet_count").toString.toDouble
        )
    )
```


## Time Types in Flink

**Flink** supports various time concepts:

- ** Processing Time**: This the time at which a record was processed on the machine (so in short words: machine time).
- **Ingestion Time**: Since on a cluster data is processed on several nodes, the time on each machine might not be 100% the same. When using **ingestion time**, a timestamp is assigned to the data when it enters the system, so even when the data is processed on various nodes, the time will be the same.
- **Event Time**: In a lot of cases processing and ingestion time are not ideal candidates - normally you might want to use a time which relates to the creation of the data (even time). In this case the timestamp is already part of the incoming data.

**Watermarks**:

To tell Flink that you want to use **event time**, add the following snippet:

```
env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
```

Unsurprisingly you need to tell Flink which field your event time is (see also [Generating Timestamps / Watermarks](https://ci.apache.org/projects/flink/flink-docs-master/dev/event_timestamps_watermarks.html), in particular [Timestamp Assigners / Watermark Generators](https://ci.apache.org/projects/flink/flink-docs-master/dev/event_timestamps_watermarks.html#timestamp-assigners--watermark-generators)).

Other resources:

- [Java Usage Example](http://apache-flink-mailing-list-archive.1008284.n3.nabble.com/Playing-with-EventTime-in-DataStreams-td10498.html)
- [Introduction to Flink Streaming - Part 9 : Event Time in Flink](http://blog.madhukaraphatak.com/introduction-to-flink-streaming-part-9/)


> **Important**: Both timestamps and watermarks are specified as **milliseconds** since the Java epoch of `1970-01-01T00:00:00Z`. (Source: [Generating Timestamps / Watermarks](https://ci.apache.org/projects/flink/flink-docs-master/dev/event_timestamps_watermarks.html)). This means we should define the value as type **Long**.

Important Info: [Pre-defined Timestamp Extractors / Watermark Emitters](https://ci.apache.org/projects/flink/flink-docs-master/dev/event_timestamp_extractors.html)

So while we managed to convert the `created_at` from `String` to `DataTime`, the missing piece is to convert this value to **milliseconds since Java epoch**. At the same time, we will also define a data model using a `case class`:

```scala
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

    val timedStream = record.assignAscendingTimestamps(_.creationTime)
```

Note that the *Ascending Timestamp* feature does not support any late arriving data.

# ElasticSearch and Kibana

We want to store our stats in a dedicated data store. InfluxDB? would be a choice, but we'll use ElasticSearch. ElasticSearch for time-series data you might wonder? Really? Turns out it's actually pretty good at handling time-series data based on [this articel](LINK MISSING!!!).

## Installing ElasticSearch

Is fully covered in [the official docu](https://www.elastic.co/guide/en/elasticsearch/reference/current/_installation.html) and also [here](https://www.elastic.co/guide/en/elasticsearch/guide/current/running-elasticsearch.html).

Here an example setup for Fedora:

[Download](https://www.elastic.co/downloads/elasticsearch)

Download the RPM file to install on Fedora.

```
sudo dnf install elasticsearch-2.4.0.rpm
```

```
bin/elasticsearch
curl -X GET http://localhost:9200/
```

OR:

```
### NOT starting on installation, please execute the following statements to configure elasticsearch service to start automatically using systemd
 sudo systemctl daemon-reload
 sudo systemctl enable elasticsearch.service
### You can start elasticsearch service by executing
 sudo systemctl start elasticsearch.service
  Verifying   : elasticsearch-2.4.0-1.noarch       
```

### Directory Structure

[Source](https://www.elastic.co/guide/en/elasticsearch/reference/1.4/setup-dir-layout.html)

Type | Description | Location Debian/Ubuntu
----|------|------
bin files | | `/usr/share/elasticsearch/bin`
config | | `/etc/elasticsearch`
data | The location of the data files of each index / shard allocated on the node. | `/var/lib/elasticsearch/data`
logs | Log files location | `/var/log/elasticsearch`

## Installing Kibana

[Download](https://www.elastic.co/downloads/kibana)

If you are using Fedora, download the RPM 64bit version to install on Fedora.

```
sudo dnf install kibana-4.6.1-x86_64.rpm
sudo systemctl start kibana.service
```
[Getting Started](https://www.elastic.co/guide/en/kibana/4.6/index.html)

- Open config/kibana.yml in an editor
- Set the elasticsearch.url to point at your Elasticsearch instance
- Run `./bin/kibana` (or `bin\kibana.bat` on Windows)
- Point your browser at `http://localhost:5601`

### Directory Structure

Type | Description | Location Debian/Ubuntu
----|------|------
root dir | | `/opt/kibana/`
bin files | | `/opt/kibana/bin`
config | | `/opt/kibana/config`
logs | Log files location | `???`


## Installing Sense

"Sense is a Kibana app that provides an interactive console for submitting requests to Elasticsearch directly from your browser."

[Installing Sense](https://www.elastic.co/guide/en/elasticsearch/guide/current/running-elasticsearch.html#sense)

```
sudo systemctl stop kibana.service
cd /opt/kibana
sudo ./bin/kibana plugin --install elastic/sense
sudo systemctl start kibana.service
```

Sense is available on `http://localhost:5601/app/sense`.

## A quick Introduction

I recommend reading the [Official Getting Started Guide](https://www.elastic.co/guide/en/elasticsearch/guide/current/getting-started.html). There is also a [forum](https://discuss.elastic.co/) available. Make sure that you understand the meaning of **index** in the context of **ElasticSearch** by reading [this section](https://www.elastic.co/guide/en/elasticsearch/guide/current/_indexing_employee_documents.html) - if you've never worked with ElasticSearch and you think you know what an index is in ElasticSearch, make sure you read the previously mentioned article! In essence, **index** is like a table in the relational world. **ElasticSearch** also uses an **inverted index** to index the document itself (ElasticSearch by default indexes every field). 

With the basics out of the way, let's test a data structure for our stream. You do not really explicitly create an **index** (table) on **ElasticSearch**, but you simply submit your document and tell **ElasticSearch** at the same time where it should be placed. If the index does not exist, it will be automatically created. Apart from the **index**, you also specify a **type** (think of it as a **partition** in the relational world) and a **unique id**, none of which have to be created upfront. The main document structure should be similar between types, but types can have some specific additional fields:

```
<index>/<type>/<unique-id>
# relational world equivalent
<table>/<partition>/<key>
```
See also [here](https://www.elastic.co/guide/en/elasticsearch/guide/current/_document_metadata.html)
Index names must be in lower case and must not start with an underscore. Type names may be lower or upper case. The Id is a **String** and can be supplied or auto-generated ([more details](https://www.elastic.co/guide/en/elasticsearch/guide/current/index-doc.html)).

So to test this, we will submit a record

If you are using **cURL**:

```
curl -PUT 'localhost:9200/_count?pretty' /twitter/tweetsByLanguage/1 [OPEN]
{
    "first_name" : "John",
    "last_name" :  "Smith",
    "age" :        25,
    "about" :      "I love to go rock climbing",
    "interests": [ "sports", "music" ]
}
```

Alternatively you can also use **Sense**:

```
PUT /twitter/tweetsByLanguage/1 [OPEN]
{
    "first_name" : "John",
    "last_name" :  "Smith",
    "age" :        25,
    "about" :      "I love to go rock climbing",
    "interests": [ "sports", "music" ]
}
```

Retrieving the document:

```
#cURL
curl XGET 'http://localhost:9200/megacorp/employee/1?pretty'
#Sense
GET /megacorp/employee/1
```

Action | Description
--------|-------
XGET    | **Retrieve** document
XPUT    | **Insert** or **update** document. Update replaces existing document
XPOST   | **Insert** document and auto-generate id for it (see [here](https://www.elastic.co/guide/en/elasticsearch/guide/current/index-doc.html))
XDELETE | **Delete** document
XHEAD   | Check if document exits

Here some more useful commands

```
# Retrieve all documents
GET /megacorp/employee/_search
# Show all documents that have a user with a last name of Smith
GET /megacorp/employee/_search?q=last_name:Smith
# Retrieve specific fields only
GET /website/blog/123?_source=title,text
```

For more complex queries, you can use the [Query DSL](https://www.elastic.co/guide/en/elasticsearch/guide/current/_search_with_query_dsl.html).

## Defining a Schema


## Flink: Using the ElasticSearch 2 connector (Sink)

In the Apache Flink 1.2 Snapshot release are ElasticSearch 1 and [ElasticSearch 2 Connectors](https://ci.apache.org/projects/flink/flink-docs-master/dev/connectors/elasticsearch2.html) included, but they are not part of the core library, so you have to add this dependency to your `build.sbt` file:

```
"org.apache.flink" %% "flink-connector-elasticsearch2"  % flinkVersion,
```

Next we have to find out some configuration details:

```bash
sudo less /etc/elasticsearch/elasticsearch.yml
```

Search for `cluster.name` ...

### First attempt insert into ElasticSearch

The connection happens over TCP, ElasticSearch usually listens on port 9300.

We will create a simple example first just storing the language field in ElasticSearch - once we have this working, we implement the full solution.

First **create the ES index** (although I previously mentioned that you don't really have to create an index separately we do have to do it here):

```bash
curl -XPUT 'http://localhost:9200/test'
```

Next let's add this code to our **Scala** file: 

First we set up the ElasticSearch connection details. Next we extract the language field from our stream. Finally we set up the **ElasticSearch Sink**. We map our stream data to a **JSON** object using the `json.map()` function.

```scala
    //load data into ElasticSearch

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
```

Run the program and kill it after a few seconds.

Check if the data is stored in ES:

```bash
curl -XGET 'http://localhost:9200/test/_search?pretty'
```

You should get data returned similar to one (after all the metadata):

```
    }, {
      "_index" : "test",
      "_type" : "test",
      "_id" : "AVelcaqwCReiv32i2XIm",
      "_score" : 1.0,
      "_source" : {
        "data" : "fr"
      }
    }, {
      "_index" : "test",
      "_type" : "test",
      "_id" : "AVelcawOCReiv32i2XIw",
      "_score" : 1.0,
      "_source" : {
        "data" : "es"
      }
```

Note that **ElasticSearch** created an id for each of our records.

### Full Sink Implementation

In order to load our `TimedStream` into **ElasticSearch**, we have to make sure that **ElasticSearch** is aware of the field types in order to optimise performance. We will create an **index mapping** (a table definition in the relational world):

```bash
# delete if already exists
curl -XDELETE 'http://localhost:9200/twitter'
# create index
curl -XPUT 'http://localhost:9200/twitter'
# create mapping
curl -XPUT 'http://localhost:9200/twitter/_mapping/languages' -d'
{
 "languages" : {
   "properties" : {
      "language": {"type": "string"}
      , "creationTime": {"type": "date"}
      , "id": {"type": "long"}
      , "retweetCount": {"type": "integer"}
    }
 } 
}'
```

We will adjust our **Scala code** now:

```scala

```

> **Important**: You must ensure that the order of the properties in the generated **JSON** object is the same as specified in the **mapping**.

If for some reason you cannot find the data on **ElasticSearch** (when you program executed without any errors), consult the **ElasticSearch Log**:

```
less /var/log/elasticsearch/elasticsearch.log
```

In my case I found a message shown below because there was a type mismatch (not because the order of the field matters):

```
[2016-10-11 17:45:09,306][DEBUG][action.bulk              ] [Sleek] [twitter][3] failed to execute bulk item (index) index {[twitter][languages][AVe0oir-LpV6XBRrSKbo], source[{"creationTime":"1476204303000","language":"ko","id":"7.8588392611606938E17","retweetCount":"0.0"}]}
MapperParsingException[failed to parse [language]]; nested: NumberFormatException[For input string: "ko"];
```

Although **elements** in a **JSON object** are by definition **unordered** (as discussed [here](http://stackoverflow.com/questions/3948206/json-order-mixed-up) and [here](http://stackoverflow.com/questions/17229418/jsonobject-why-jsonobject-changing-the-order-of-attributes)), JSON libraries can arrange the elements in any particular order (which means not necessarily in the order we specified them). The **ElasticSearch Mapping** we created earlier on does not require the elements have to be **ordered** either. However, if for some reason you want to maintain the insert order in the map, in **Scala**, we can use an immuatable `ListMap` to just achieves this (as well as the Java `LinkedHashMap`).

Check if the data is stored in ES:

```bash
curl -XGET 'http://localhost:9200/twitter/languages/_search?pretty'
```

The result should look something like this (after all the metadata):

```
}, {
      "_index" : "twitter",
      "_type" : "languages",
      "_id" : "AVe00lLMLpV6XBRrSKz5",
      "_score" : 1.0,
      "_source" : {
        "creationTime" : "1476207461000",
        "language" : "en",
        "id" : "7.8589717172390707E17",
        "retweetCount" : "0.0"
      }
    }, {
```

# Sources

- [Elasticsearch as a Time Series Data Store](https://www.elastic.co/blog/elasticsearch-as-a-time-series-data-store)
- [Pre-defined Timestamp Extractors / Watermark Emitters](https://ci.apache.org/projects/flink/flink-docs-master/dev/event_timestamp_extractors.html)
- [READ: Building a demo application with Flink, Elasticsearch, and Kibana](https://www.elastic.co/blog/building-real-time-dashboard-applications-with-apache-flink-elasticsearch-and-kibana) with [Scala Code on Github](https://github.com/dataArtisans/flink-streaming-demo/tree/master/src/main/scala/com/dataartisans/flink_demo)
- [Java Connector Example](http://dataartisans.github.io/flink-training/exercises/toElastic.html)
- [READ THIS](http://apache-flink-user-mailing-list-archive.2336050.n4.nabble.com/Handling-large-state-incremental-snapshot-td5916.html)
- [READ: Flink Complex Event Processing](http://www.slideshare.net/flink-taiwan/complex-event-processing-use-cases-flinkcep-library-flinktw-meetup-20160719)
- [READ: Flink Kafka Consumer Scala Example](http://stackoverflow.com/questions/31446374/can-anyone-share-a-flink-kafka-example-in-scala), and also [this](http://apache-flink-user-mailing-list-archive.2336050.n4.nabble.com/Flink-Kafka-example-in-Scala-td2069.html)

