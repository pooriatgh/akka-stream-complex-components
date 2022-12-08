import akka.event.LoggingAdapter
import akka.{ Done, NotUsed }
import akka.stream.Supervision.{ Resume, Stop }
import akka.stream.alpakka.file.DirectoryChange
import akka.stream.alpakka.file.scaladsl.{ Directory, DirectoryChangesSource, FileTailSource }
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Framing.FramingException
import akka.stream.{
  ActorAttributes,
  ClosedShape,
  FlowShape,
  Graph,
  Inlet,
  Materializer,
  SinkShape,
  SourceShape,
  UniformFanOutShape
}
import akka.stream.scaladsl.{ Balance, Broadcast, Flow, GraphDSL, Merge, MergeHub, RunnableGraph, Sink, Source }

import java.io.{ File, FileNotFoundException }
import java.nio.file.{ FileSystems, Path }
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.*
import scala.util.{ Failure, Success }

//-Dconfig.file =
case class FileSource(
    maxLineSize: Int = 8192,
    pollingInterval: FiniteDuration = 250.millis,
    shutDownWhenDeleted: Boolean = true
)(implicit materializer: Materializer, ex: ExecutionContext, logger: LoggingAdapter) {

  private def toPath: String => Path =
    path =>
      val fs = FileSystems.getDefault
      fs.getPath(path)

  private def fileCheckSource(path: Path): Source[Nothing, NotUsed] =
    DirectoryChangesSource(path.getParent, 1.second, 8192)
      .collect {
        case (p, DirectoryChange.Deletion) if path == p =>
          throw new FileNotFoundException(path.toString)
      }
      .recoverWithRetries(
        1,
        { case _: FileNotFoundException =>
          Source.empty
        }
      )

  private def fileTailSource(path: Path): Source[String, NotUsed] = FileTailSource
    .lines(
      path = path,
      maxLineSize = maxLineSize,
      pollingInterval = pollingInterval
    )
    .recoverWithRetries(
      1,
      { case _: RuntimeException =>
        println(s"max line size: $maxLineSize")
        FileSource(maxLineSize * 2).fileTailSource(path)
      }
    )
    .log("file source")

  def singleFileSource(stringPath: String): Source[String, NotUsed] = Source.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>

      import GraphDSL.Implicits._

      val path = toPath(stringPath)

      val lines: Source[String, _] = fileTailSource(path).watchTermination() { (_, done) =>
        done.onComplete {
          case Success(_) =>
            logger.info("source completed successfully")
          case Failure(e) =>
            logger.error(s"source completed with failure : $e")
        }
      }

      val out: Source[String, _] =
        if shutDownWhenDeleted then lines.merge(fileCheckSource(path), eagerComplete = true)
        else lines

      SourceShape.of(builder.add(out).out)
    }
  )

  def multipleFileSource[M](root: String, sink: Sink[String, M]): RunnableGraph[NotUsed] =
    RunnableGraph.fromGraph(
      GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._

        val rootPath: Path = toPath(root)

        val mergeHub                    = MergeHub.source[String]
        val port: Sink[String, NotUsed] = mergeHub.to(sink).run()

        val newFiles = DirectoryChangesSource(rootPath, pollInterval = 1.second, maxBufferSize = 1000)
          .filter { case (path, _) =>
            !path.endsWith("~")
          }
          .collect { case (path, DirectoryChange.Creation) =>
            logger.warning(path.toString)
            path
          }

        Directory
          .ls(rootPath)
          .merge(newFiles)
          .filter(p => new File(p.toString).isFile)
          .runForeach(p => singleFileSource(p.toString).runWith(port))


        ClosedShape
      }
    )
  // current problem :: Illegal GraphDSL usage. Inlets [Map.in] were not returned in the resulting shape and not connected.
}
