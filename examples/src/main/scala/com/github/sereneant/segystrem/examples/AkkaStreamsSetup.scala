package com.github.sereneant.segystrem.examples

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.scaladsl.{FileIO, Source}
import akka.stream.{ActorMaterializer, IOResult}
import akka.util.ByteString

import scala.concurrent._

class AkkaStreamsSetup extends App {

  private val path = args match {
    case Array(argPath, _*) => Paths.get(argPath)
    case _ => throw new RuntimeException(s"SegY file path should be first param in args")
  }

  // Akka streams setup
  implicit val system: ActorSystem = ActorSystem("segystream-examples")
  implicit val mat: ActorMaterializer = ActorMaterializer()

  // Construct stream source from file
  val fileSource: Source[ByteString, Future[IOResult]] = FileIO.fromPath(path)
}
