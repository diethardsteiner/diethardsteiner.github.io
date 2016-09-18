---
layout: post
title: "Apache Flink Streaming: Simple Twitter Example"
summary: This article provides a short intro into the fascinating world of Apache Flink
date: 2016-09-18
categories: Flink
tags: Flink
published: false
--- 

## Using the latest snapshot

### The SBT Build File

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

### Coding in IntelliJ IDEA

Since we appreciate the convenience that an IDE provides, we will add our **SBT** project as a new project to **IDEA**.

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

We setup a `TwitterSource` reader proving all the authentication details (as described in [the official docu](https://ci.apache.org/projects/flink/flink-docs-master/dev/connectors/twitter.html)). Compile and execute our program by clicking onto the **Run** button. This emmits a string with random Twitter records in **JSON** format, e.g.:

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

The next task is to convert the stringified JSON to a proper Scala representation: As suggested by [this Stackoverflow](http://stackoverflow.com/questions/30884841/converting-json-string-to-a-json-object-in-scala) answer we will could make use of the [ScalaJson](https://www.playframework.com/documentation/2.5.x/ScalaJson) library from the **Play Framework** to do this and add this dependency to our `build.sbt` file:

```
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.4.8"
```

Link to [repo](https://mvnrepository.com/artifact/com.typesafe.play/play-json_2.10/2.4.8). However, we can just use the built in Scala library for now as well.

We will use the native **Scala JSON parser** (`scala.util.parsing.json._`) to turn the String into a proper collection. For now we will simply use a map function, check that the parsing is successful (using a case statement??) and then simply print out the result:

```scala
import java.util.Properties

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.connectors.twitter.TwitterSource

import scala.util.parsing.json._
//import play.api.libs.json._

object FlinkTwitterStreamCount {
  def main(args: Array[String]) {

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val props = new Properties()
    props.setProperty(TwitterSource.CONSUMER_KEY, "<your-value>")
    props.setProperty(TwitterSource.CONSUMER_SECRET, "<your-value>")
    props.setProperty(TwitterSource.TOKEN, "<your-value>")
    props.setProperty(TwitterSource.TOKEN_SECRET, "<your-value>")
    val streamSource = env.addSource(new TwitterSource(props))

    streamSource.map( value => {

        val result = JSON.parseFull(value)

        result match {
          case Some(e:Map[String, Any]) => println(e)   // println(e("retweeted"))
          case None => println("Error")
        }

      }
    )

    env.execute("Twitter Window Stream WordCount")
  }
}
```

The next step is check filter out some records and convert the `Map` to `Tuple`, allowing us to keep certain fields only:

```scala
 val jsonMap = streamSource.map(

      value => {

        val result = JSON.parseFull(value)

        result match {
//          case Some(e: Map[String, Any]) => println(e)
//          case None => println("Error")
          case Some(e: Map[String, Any]) => e
        }
      }
    )

    val filteredStream = jsonMap.filter( value =>  value.contains("created_at"))

    val record:DataStream[Tuple3[String, BigDecimal, BigDecimal]] = filteredStream.map(
      value => (
          value("created_at").toString
          , BigDecimal(value("id").toString)
          , BigDecimal(value("retweet_count").toString)
        )
    )
```

