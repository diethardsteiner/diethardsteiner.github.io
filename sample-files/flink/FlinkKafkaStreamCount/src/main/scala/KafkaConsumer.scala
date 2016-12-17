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
    // comma separated list of Zookeeper servers, only required for Kafka 0.8
    //properties.setProperty("zookeeper.connect", "localhost:2181")
    // id of the consumer group
    properties.setProperty("group.id", "test")
    val stream = env
      // words is our Kafka topic
      .addSource(new FlinkKafkaConsumer010[String]("words", new SimpleStringSchema(), properties))

//    stream.print

    val counts = stream
      .map { (_, 1) }
      .keyBy(0)
      .timeWindow(Time.seconds(5))
      .sum(1)

    counts.print

    env.execute("Kafka Window Stream WordCount")
  }
}

