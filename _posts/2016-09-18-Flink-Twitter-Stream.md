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


