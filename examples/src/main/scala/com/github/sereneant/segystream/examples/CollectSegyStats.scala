package com.github.sereneant.segystream.examples

import java.io.{ByteArrayOutputStream, PrintStream}
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger

import akka.Done
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.github.sereneant.segystream._
import org.HdrHistogram.ConcurrentDoubleHistogram

import scala.concurrent.{ExecutionContextExecutor, Future}

// Collects Seg-Y file statistics.
object CollectSegyStats extends AkkaStreamsSetup {

  // Declare the stream
  val segySource: Source[SegyPart, Future[SegyHeaders]] = fileSource.viaMat(SegyFlow())(Keep.right)

  val stats = new SegyStats

  // Run the stream
  val done: Future[Done] = segySource
    .map(stats.collect) // collect the statistics
    .toMat(Sink.ignore)(Keep.right) // wait for the Sink to complete
    .run()

  // Wait for stream termination and print the stats
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  done.onComplete { _ =>
    system.terminate()
    println(stats.info)
  }

  /**
    * Thread-safe Seg-Y data statistics with Gil Tene's HdrHistogram (just for fun)
    * https://github.com/HdrHistogram/HdrHistogram
    */
  class SegyStats {
    val traces = new AtomicInteger(0)
    val traceChunks = new AtomicInteger(0)
    val histogram = new ConcurrentDoubleHistogram(3) //TODO: Make it configurable!!!

    def collect(segy: SegyPart): Unit = segy match {
      case th: TraceHeader => traces.incrementAndGet()

      case td: TraceDataChunk =>
        traceChunks.incrementAndGet()
        td.floatIterator.map(_.toDouble * 1000000).foreach(histogram.recordValue)
      case _ => // NoOp
    }

    def info: String = {
      val baos = new ByteArrayOutputStream
      try {
        val ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name)
        try {
          histogram.outputPercentileDistribution(ps, 1.0)
        } finally {
          if (ps != null) ps.close()
        }
      } catch {
        case ex: Throwable => ex.printStackTrace()
      }
      val histogramInfo = new String(baos.toByteArray, StandardCharsets.UTF_8)

      s"SegyStats:\n" +
        s"  traces: ${traces.get}, traceChunks: ${traceChunks.get} \n" +
        s"  HdrHistogram:\n $histogramInfo"
    }
  }
}