---
layout: post
title: "Real Time Streaming with Apache Flink and Kafka: Simple Example"
summary: This article provides a short intro into the fascinating world of Apache Flink
date: 2016-12-08
categories: Flink
tags: Flink
published: true
--- 

Last Tuesday I attended the **Apache Flink Meetup** here in London for a coding dojo. The previous coding dojo was really very interesting and I went away with some good learnings - it also provided me with motivation to look a bit more into **Apache Flink** and I eventually published two blog posts on this topic. So I was quite excited to attend [this new coding dojo event](https://www.meetup.com/Apache-Flink-London-Meetup/events/235900942/). Unfortunately this time round the attendency rate was not so good, however, event organiser Ignas Vadaisa and I still went ahead and tried to get a very simple **Kafka** and **Flink** setup going, the result of which is discussed here.

The main idea was to set up a simple **Kafka Producer** (Ignas wrote a Scala object which sends a random pick from a set of words to a Kafka topic), I set up a local installation of Kafka and wrote a simple **Kafka Consumer**, which is using **Flink** to do a word count.

I will talk you through the setup now, which is rather straight forward and should be a starting point for furhter exploration:

Check the scala version you have installed

```bash
$ scala -version
```

Download **Kafka** from [here](https://kafka.apache.org/downloads) and extract the file in a convenient directory. Make sure you choose a Kafka version which goes in line with your Scala version!

Next we follow mainly the [Kafka Quickstart](https://kafka.apache.org/documentation.html#quickstart) guide:


Start **Zookeeper**:

```bash
$ bin/zookeeper-server-start.sh config/zookeeper.properties
```

Start **Kafka**:

```bash
$ bin/kafka-server-start.sh config/server.properties
```

Create a **topic**: This is not strictly necessary, as our code (`KafkaProducer`) will take care of this any ways.

```bash
$ bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic words
```

List the topic:

```bash
$ bin/kafka-topics.sh --list --zookeeper localhost:2181
words
```

Let's first create the **Kafka Producer**. We will generate some random data and send it to the messaging queue:

```scala
import java.util.Properties
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import scala.util.Random

object KafkaProducer extends App {

  val topic = "words"
  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")


  val rnd = new Random()
  val wordSet = Seq("Dog", "Cat", "Cow")
  val n = wordSet.length

  val producer = new KafkaProducer[String,String](props)

  var key  = 0

  while(true){
    val index = rnd.nextInt(n)
    producer.send(new ProducerRecord(topic, key.toString, wordSet(index)))
    key = key + 1
  }

  producer.close
}
```

Let's run this code. To understand if our code is actually working, let's use a **Kafka utility** to check if there are any messages in the queue:


Show streaming data in the Kafka topic:

```bash
bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic words
```

Since the setup is working so far, we can focus on creating the `KafkaConsumer` now. In the snapshot 1.2 Flink provides quite few **connectors** to external data sources and stores, which makes it quite straight forward to source data from and load data to such stores:

```scala
import java.util.Properties
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.connectors.kafka._
import org.apache.flink.streaming.util.serialization.SimpleStringSchema

object KafkaConsumer {


  def main(args: Array[String]) {

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val properties = new Properties()
    // comma separated list of Kafka brokers
    properties.setProperty("bootstrap.servers", "localhost:9092")
    // id of the consumer group
    properties.setProperty("group.id", "test")
    val stream = env
      // words is our Kafka topic
      .addSource(new FlinkKafkaConsumer010[String]("words", new SimpleStringSchema(), properties))

    stream.print

    env.execute("Kafka Window Stream WordCount")
  }
}
```

In this first attempt we simply retrieve the data from the **Kafka** queue and print the values out to the console. Try to run this code.

Next let's count the words in five seconds intervals:

```scala
    val counts = stream
      .map { (_, 1) }
      .keyBy(0)
      .timeWindow(Time.seconds(5))
      .sum(1)

    counts.print
```

Execute `KafkaConsumer` now and enjoy the **window aggregation**! Granted this was a very simple setup, so feel free to explore more of the **Apache Flink** world and create an advanced example. Have fun!

