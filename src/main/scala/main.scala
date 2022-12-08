import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.scaladsl.Source
import fileTools.FileSource
import tasks.FileToKafka
import io.circe.generic.auto.*
import io.circe.syntax.*

import scala.concurrent.ExecutionContext

@main
def main(): Unit = {

  given sys: ActorSystem       = ActorSystem("teststream")
  given exec: ExecutionContext = sys.dispatcher
  given logger: LoggingAdapter = sys.log

  case class OHLCV(
      open: BigDecimal,
      high: BigDecimal,
      low: BigDecimal,
      close: BigDecimal,
      volume: BigDecimal,
      timestamp: Long,
      market: String
  )
  object OHLCV {
    def apply(strs: Array[String], market: String): OHLCV =
      if (strs.length >= 6)
        OHLCV(
          BigDecimal(strs(1)),
          BigDecimal(strs(2)),
          BigDecimal(strs(3)),
          BigDecimal(strs(4)),
          BigDecimal(strs(5)),
          strs(0).toLong,
          market
        )
      else
        throw new RuntimeException(s"this str:${strs.fold("")(_ + "," + _)} does not contain all fields to fill ohlcvt")
  }

  def csvToJsonOhlcv: String => String = row => OHLCV(row.split(","), "eth-usd").asJson.toString
  val path                             = "src/main/resources/kraken/ETH_OHLCVT/ETHUSD_5.csv"
  FileToKafka(path, "ohlcvt")(csvToJsonOhlcv).run

}
