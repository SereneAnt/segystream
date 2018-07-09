package com.github.sereneant.segystream.examples

import akka.stream.scaladsl.{Keep, Sink, Source}
import com.github.sereneant.segystream.{SegyFlow, SegyHeaders, SegyPart}

import scala.concurrent._

/**
  * Retrieves Seg-Y headers and terminates the stream.
  */
object GetSegyHeaders extends AkkaStreamsSetup {

  // Declare the stream
  val segy: Source[SegyPart, Future[SegyHeaders]] = fileSource.viaMat(SegyFlow())(Keep.right)

  // Run the stream
  val done: Future[SegyHeaders] = segy.to(Sink.ignore).run()

  // Wait for headers and terminate the rest of the stream
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  done.onComplete (_ => system.terminate())

  // Print headers info
  done.foreach { segyHeaders =>
    println(segyHeaders.textHeader.info)
    println(segyHeaders.binHeader.info)
    segyHeaders.extTextHeaders.foreach(extTextHeader =>
      println(extTextHeader.info)
    )
  }

}
