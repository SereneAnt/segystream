package com.github.sereneant.segystream.examples

import akka.Done
import akka.stream.scaladsl.Keep._
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.github.sereneant.segystream._

import scala.concurrent.{ExecutionContextExecutor, Future}

// Retrieves slice data by given xLine
object GetDataForSlice extends AkkaStreamsSetup {
  val SliceXLine = 0

  // Declare the stream
  val segySource: Source[SegyPart, Future[SegyHeaders]] = fileSource.viaMat(SegyFlow())(right)

  // Run the stream
  val done: Future[Done] = segySource
    .collect {
      case trace: TraceDataChunk if trace.xLine == SliceXLine => trace
    }
    .runForeach(chunk => println(chunk.info)) //Output collected data to console

  // Wait for stream termination and print the stats
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  done.onComplete { _ => system.terminate() }

}
