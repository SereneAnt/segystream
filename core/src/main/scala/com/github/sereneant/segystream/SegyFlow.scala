package com.github.sereneant.segystream

import java.nio.charset.Charset

import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.util.ByteString

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Future, Promise}

case class SegyConfig(
  charset: Charset = Charset.forName("CP037"), //textual data charset
  dataChunkSize: Int = 1024 //bytes
)

object SegyFlow {
  def apply(): SegyFlow = new SegyFlow(SegyConfig()) //Default config
}

class SegyFlow(cfg: SegyConfig)
  extends GraphStageWithMaterializedValue[FlowShape[ByteString, SegyPart], Future[SegyHeaders]] {

  val in: Inlet[ByteString] = Inlet("SegyIn")
  val out: Outlet[SegyPart] = Outlet("SegyOut")
  override val shape: FlowShape[ByteString, SegyPart] = FlowShape(in, out)

  override def createLogicAndMaterializedValue(attrs: Attributes): (GraphStageLogic, Future[SegyHeaders]) = {
    val promise = Promise[SegyHeaders]()
    val matBuffer = ListBuffer.empty[SegyPart] //Mutable!!!

    val logic: GraphStageLogic = new GraphStageLogic(shape) {
      private[this] var phase: SegyPhase = TextHeaderPhase(cfg)
      private[this] var buffer = ByteString.empty

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          buffer = buffer ++ grab(in)
          while(buffer.length >= phase.length) {
            val (currBuff, nextBuff) = buffer.splitAt(phase.length)
            val (segy, nextPhase) = phase.extract(currBuff)
            if (!promise.isCompleted) { //It's OK to use isCompleted here, no race conditions
              if (phase.matPromise == KEEP) matBuffer += segy
              if (nextPhase.matPromise == COMPLETE) promise.success(SegyHeaders.of(matBuffer))
            }
            emit(out, segy)
            phase = nextPhase
            buffer = nextBuff
          }
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          pull(in)
        }
      })
    }

    (logic, promise.future)
  }
}