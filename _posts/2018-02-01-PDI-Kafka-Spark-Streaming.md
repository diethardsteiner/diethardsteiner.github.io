---
layout: post
title: "Pentaho Data Integration v8: Real-time Streaming with Kafka and Spark"
summary: This article explains how to easily create a PDI job to stream data into HDFS via Kafka and Hadoop
date: 2018-02-01
categories: PDI
tags: PDI, Spark, Kafka
published: false
---

Kafka is everywhere these days! So it is about time that we take a look at how to populate a Kafka stream (aka topic) with PDI and how to fetch the data on the other side.

## Starting Kafka

Download Kafka from [here](https://kafka.apache.org/downloads) and extract the file in a convenient directory. Make sure you choose a Kafka version which goes in line with your Scala version!

Next we follow mainly the [Kafka Quickstart guide](https://kafka.apache.org/documentation.html#quickstart):

Start **Zookeeper**:

```bash
./bin/zookeeper-server-start.sh config/zookeeper.properties
```

Start **Kafka**:

```bash
./bin/kafka-server-start.sh config/server.properties
```

## To create a topic upfront or not

You can use the `kafka` command to create a dedicated topic upfront, e.g. `words`:

```bash
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic words
```

However, this is not really necessary, because the the PDI **Kafka Consumer** step will automatically create the topic if it doesn't exist.

## How to configure the PDI Kafka Consumer step

Add following properties to `kettle.properties`:

```
PROP_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
PROP_KAFKA_TOPIC=words
PROP_OUTPUT_DIR=/tmp
```

Start PDI **Spoon**.

In PDI **Spoon** create a transformation called `tr_publish_to_kafka_topic` and add following steps (connect all of them):

- **Generate Rows**: Add a field called topic of type String and set its value to `words`. Also tick **Never stop generating rows**.
- **Add sequence**: Set the **Name of the value** to `key`.
- **Generate random value**: create a new field called `word` and set the algorithm to **random string**.
- **Kafka Producer**

![](/images/pdi-kafka-streaming/tr_publish_to_kafka_topic.png)

Configure the **Kafka Producer** step as follows:

- **Connection**: If you are running **Kafka** in **standalone** mode, like we do, then tick **Direct**, otherwise, if you are running it on a cluster, tick **Cluster**. When using the **Cluster** option, the **Big Data Shim** for PDI has to be configured. Then specify the **Bootstrap Servers**: In our case, this should be set to `localhost:9092` or alternatively define a parameter.
- **Client ID**: This can be set to any unique value.
- **Topic**: In PDI v8 it seems like you should be able to pick this field from the incoming stream, however, in my case this did not work reliably. You can hard code value or use a parameter. In our case this should be set to `words`  or alternatively define a parameter.
- **Key field**: Pick the relevant field from the incoming stream, `key` in our case. Important point: "In Kafka, all messages can be keyed, allowing for messages to be distributed to partitions based on their keys in a default routing scheme. If no key is present, messages are randomly distributed to partitions." ([Source](https://help.pentaho.com/Documentation/8.0/Products/Data_Integration/Transformation_Step_Reference/Kafka_Producer))
- **Message field**: Pick the relevant field from the incoming stream, `word` in our case.

The **Options** tab allows you to specify some custom configuration details if required. We don't change it.

![](/images/pdi-kafka-streaming/kafka-producer.png)

> **Note**: If your Kafka topic supports partitions, there does not seem to be a way in PDI to send data to a specific partition. Unless you can set this via *Options*? [OPEN]

Before we move on to create the Kafka Consumer, we shall take a look at some interesting command line options:

**List Kafka topics**:

```
$ ./bin/kafka-topics.sh --list --zookeeper localhost:2181
words
```

**List Details for a given Kafka topic**, e.g. the number of partitions ([Source](https://stackoverflow.com/questions/35437681/kafka-get-partition-count-for-a-topic)):

```bash
$ ./bin/kafka-topics.sh --describe --zookeeper localhost:2181 --topic words
Topic:words	PartitionCount:1	ReplicationFactor:1	Configs:
	Topic: words	Partition: 0	Leader: 0	Replicas: 0	Isr: 0
```

## How to check if the Topic gets populated

An old English saying goes like this: "The proof is in the pudding". And indeed, so it is: Let's check if our records show up in the **Kafka topic** (stream):

```
$ bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic words
2bmdfjlrc34gb
7r8ndcdfg4e0f
1pofv11k1sbg3
7ru4lfcdomrco
5q1d34a9c4k57
6bvtece5iflom
```


## Creating the Kafka Consumer

Create a transformation called `tr_consume_from_kafka_topic` and add a **Kafka Consumer** step. That's it. Extremely simple. This transformation will just initiate the connection to the Kafka topic and pull records. The **Kafka Consumer** step will call a subtransformation every x milliseconds or every x records (batch window).

![](/images/pdi-kafka-streaming/tr_consume_from_kafka_topic.png)

Create another transformation called `tr_get_records_from_stream`, which will serve as a sub-transformation to `tr_consume_from_kafka_topic`.

Configure the **Kafka Consumer** step in `tr_consume_from_kafka_topic` as follows:

Reference the `tr_get_records_from_stream` transformation.

**Setup** tab:

- **Connection**: If you are running **Kafka** in **standalone** mode, like we do, then tick **Direct**, otherwise, if you are running it on a cluster, tick **Cluster**. When using the **Cluster** option, the **Big Data Shim** for PDI has to be configured. Then specify the **Bootstrap Servers**: In our case, this should be set to `localhost:9092` or alternatively define a parameter.
- **Topics**: Provide the name of the topic, in our case `words` or alternatively define a parameter.
- **Consumer Group**: Provide any unique name.

**Batch** tab:

Here you can either define the **Number of records** or the **Duration (ms)**, which will create batches of the incoming stream and send it to the sub-transformation we defined earlier on. You can leave the defaults for now.

**Fields** tab: Maps the incoming stream to the outgoing one and defines the data type of the fields. You can leave the defaults for now.

**Options**: Provides to option to set a few custom configurations. You can leave the defaults for now.

![](/images/pdi-kafka-streaming/kafka-consumer.png)

Next, for the `tr_get_records_from_stream` transformation, add the **Get records from stream** step and connect it up to a **Text file output** step:

![](/images/pdi-kafka-streaming/tr_get_records_from_stream.png)

For the **Get records from stream** step just define the same fields as we defined previously in the **Kafka Consumer** step:

![](/images/pdi-kafka-streaming/get-records-from-stream.png)

For the **Text file output** step, apart from the standard configuration details, tick as well **Include date in filename?** and **Include time in filename?**: Since these files get generated in batches, this helps us to generate unique file names.

![](text-file-output.png)

In the **Fields** tab click on **Get Fields**:

![](/images/pdi-kafka-streaming/text-file-output-fields.png)

## Executing the Kafka Consumer

Since we defined a **batch window** of 1000 records, we should see in the **execution log** that PDI creates **new output files** every 1000 records:

```
2018/02/01 20:19:35 - Get records from stream.0 - Finished processing (I=0, O=0, R=1000, W=1000, U=0, E=0)
2018/02/01 20:19:35 - Text file output.0 - Finished processing (I=0, O=1001, R=1000, W=1000, U=0, E=0)
2018/02/01 20:20:23 - tr_get_records_from_stream - Dispatching started for transformation [tr_get_records_from_stream]
2018/02/01 20:20:23 - Get records from stream.0 - Finished processing (I=0, O=0, R=1000, W=1000, U=0, E=0)
2018/02/01 20:20:23 - Text file output.0 - Finished processing (I=0, O=1001, R=1000, W=1000, U=0, E=0)
2018/02/01 20:21:13 - tr_get_records_from_stream - Dispatching started for transformation [tr_get_records_from_stream]
2018/02/01 20:21:13 - Get records from stream.0 - Finished processing (I=0, O=0, R=1000, W=1000, U=0, E=0)
2018/02/01 20:21:13 - Text file output.0 - Finished processing (I=0, O=1001, R=1000, W=1000, U=0, E=0)
```

We can also check this via the command line:

```bash
$ ll /tmp/output_*
-rw-rw-r--. 1 dsteiner dsteiner 10004 Feb  1 20:19 /tmp/output_20180201_201935.txt
-rw-rw-r--. 1 dsteiner dsteiner 10000 Feb  1 20:20 /tmp/output_20180201_202023.txt
-rw-rw-r--. 1 dsteiner dsteiner 10000 Feb  1 20:21 /tmp/output_20180201_202113.txt
```

If we inspect the contents of one of the files, it will look something like this:

```
key;message;topic;partition;offset
2305;73a6t9h9h50on;words;0;5429
2306;3q5cgseumehmj;words;0;5430
2307;6omkpcbjk5892;words;0;5431
```

There are some interesting command line options:

**List Consumer Groups**:

```bash
$ ./bin/kafka-consumer-groups.sh --zookeeper localhost:2181 --list
Note: This will only show information about consumers that use ZooKeeper (not those using the Java consumer API
```

Oh well, the returned message gives it away ...

**Describe the consumer group**:

```bash
$ ./bin/kafka-consumer-groups.sh --zookeeper localhost:2181 --describe --group C1
```


# Order

It is important to understand that Kafka only guarantees to keep the order within each partition (and not across all partitions). If you want to maintain the records in order, you have to option to either create just one partition or create the same number of consumers as partitions (although in the last case you still don't have the records ordered across all partitions).

# Errors And Solutions

zookeeper log:

```
brokers Error:KeeperErrorCode = NodeExists for /brokers
...
Error:KeeperErrorCode = NodeExists for /consumers
```

[Solution](https://stackoverflow.com/questions/34393837/zookeeper-kafka-error-keepererrorcode-nodeexists)

I deleted all files from `/tmp/zookeeper` and `/tmp/kafka-logs`. 
Next you will have to kill the currently running Zookeeper and Kafka processes (cancelling out of them is not enough).