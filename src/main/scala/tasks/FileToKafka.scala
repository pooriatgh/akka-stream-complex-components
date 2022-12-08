package tasks

import akka.NotUsed
import akka.event.LoggingAdapter
import akka.stream.Materializer
import fileTools.FileSource
import kafkaTools.KafkaSink

import scala.concurrent.ExecutionContext

case class FileToKafka(path: String, topic: String)(serde: String => String)(implicit
    materializer: Materializer,
    ex: ExecutionContext,
    logger: LoggingAdapter
) {
  def run = FileSource().singleFileSource(path).map(serde).to(KafkaSink(topic).sink).run()
}
