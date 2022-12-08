package kafkaTools

import akka.NotUsed
import akka.event.LoggingAdapter
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.{Inlet, SinkShape}
import akka.stream.scaladsl.{Concat, Flow, GraphDSL, Merge, Sink, Source}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
case class KafkaSink(topic: String)(implicit logger: LoggingAdapter) {

  private val conf: Config = ConfigFactory.load().getConfig("akka.kafka.producer")
  private val producerSettings: ProducerSettings[String, String] =
    ProducerSettings(conf, new StringSerializer, new StringSerializer)

  val sink: Sink[String, NotUsed] =
    Flow[String]
      .map(new ProducerRecord[String, String](topic, _))
      .wireTap(r => logger.info(r.toString))
      .to(Producer.plainSink(producerSettings))

}
