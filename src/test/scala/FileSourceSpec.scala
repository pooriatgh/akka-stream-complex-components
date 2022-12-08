import akka.actor.ActorSystem
import akka.pattern
import akka.stream.Attributes.Attribute
import akka.stream.alpakka.xml.*
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.TestKit
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

class FileSourceSpec extends TestKit(ActorSystem("testSystem")) with AnyWordSpecLike {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  "Balance" must {
    "Print parallel" in {

    }
  }
}
