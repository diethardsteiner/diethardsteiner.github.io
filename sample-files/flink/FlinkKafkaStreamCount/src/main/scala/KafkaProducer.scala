import java.util.Properties
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import scala.util.Random

object KafkaProducer extends App {

  val topic = "words"
  val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
//  props.put("acks", "all")
//  props.put("retries", 0)
//  props.put("batch.size", 16384)
//  props.put("linger.ms", 1)
//  props.put("buffer.memory", 33554432)
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")


  val rnd = new Random()
  val wordSet = Seq("Dog", "Cat", "Cow")
  val n = wordSet.length

  // https://kafka.apache.org/0100/javadoc/org/apache/kafka/clients/producer/KafkaProducer.html
  // https://gist.github.com/fancellu/f78e11b1808db2727d76
  val producer = new KafkaProducer[String,String](props)

  var key  = 0

  while(true){
    val index = rnd.nextInt(n)
    producer.send(new ProducerRecord(topic, key.toString, wordSet(index)))
    key = key + 1
  }

  producer.close
}