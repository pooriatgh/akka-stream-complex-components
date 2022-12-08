import akka.actor.ActorSystem
import akka.stream.scaladsl.Source

import scala.concurrent.ExecutionContext

@main
def main(): Unit = {

  given sys: ActorSystem = ActorSystem("teststream")
  given exec: ExecutionContext = sys.dispatcher

  Source(1 to 10)
    .groupBy(maxSubstreams = 2, _ % 2) // create two sub-streams with odd and even numbers
    .reduce(_ + _) // for each sub-stream, sum its elements
    .mergeSubstreams // merge back into a stream
    .runForeach(println)
  println("Hello world!")
}
