---
layout: post
title: "Real Time Streaming with Apache Flink and Kafka: Simple Example"
summary: This article provides a short intro into the fascinating world of Apache Flink
date: 2016-12-08
categories: Flink
tags: Flink
published: false
--- 

[Gradle](https://docs.gradle.org/current/userguide/scala_plugin.html)

https://github.com/DataReplyUK/FlinkGroupLondon -> WordCount


Download Kafka

https://kafka.apache.org/downloads

Check the scala version you have installed

```bash
scala -version
```

[Kafka Quickstart](https://kafka.apache.org/documentation.html#quickstart)


Start Zookeeper:

```bash
bin/zookeeper-server-start.sh config/zookeeper.properties
```

Start Kafka:

```bash
bin/kafka-server-start.sh config/server.properties
```

Create topic: This is not strictly necessary, as our code (KafkaProducer) will take care of this any ways.

```bash
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic words
```
List topic:

```bash
bin/kafka-topics.sh --list --zookeeper localhost:2181
```

[Scala Producer and Consumer Examples](https://github.com/smallnest/kafka-example-in-scala)

[Other Scala Producer](https://github.com/elodina/scala-kafka/blob/master/src/main/scala/KafkaProducer.scala)


Let's first create the **Kafka Producer**. We will generate some random data and send it to the messaging queue. ...

To understand if our code is actually working, let's use a **Kafka utility** to check if there are any messages in the queue:


Show Kafka topic messages:

```bash
bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic words
```
