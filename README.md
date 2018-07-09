# SegyStream

[![TravisCI build](https://travis-ci.org/SereneAnt/segystream.svg?branch=master)](https://travis-ci.org/SereneAnt/segystream)

Reactive streaming SEG-Y parser

## Features
* [Seg-Y version 1](https://seg.org/Portals/0/SEG/News%20and%20Resources/Technical%20Standards/seg_y_rev1.pdf) format supported.
* Supports asynchronous stream processing with non-blocking adaptive pull/push back pressure, as it declared by [Reactive Streams](http://www.reactive-streams.org/).
* Built with [Akka Streams](https://doc.akka.io/docs/akka/2.5/stream/index.html).
* Contains examples of different use cases: streaming from file source,
  [AWS S3](aws.amazon.com/s3), transformation, visualization, statistics, parallel processing, etc.
* API for both [scala](https://www.scala-lang.org/) and [java](https://docs.oracle.com/javase/8/docs/technotes/guides/language/index.html) languages.

## Further work
* Configurable segy data chunk size and text reading encoding
* Add Github badges - code coverage, stable version, etc
* Add more examples for streaming from file source, S3, transformation, visualization, parallel processing
* Add benchmarks, taking commonly used Seg-Y parsers as a baseline
* Add full support for set of Seg-Y v1 features (variable ext text headers, etc.)
* Add Seg-Y v2 support
* Cross-validation against other commonly used Seg-Y parsers

## Prerequisites
* java 1.8
* [sbt](https://www.scala-sbt.org/) 1.x

## How to use
Add dependency:

**Sbt**
```sbt
libraryDependencies += "com.github.sereneant.segystream" %% "core" % "0.1.0"
```

**Maven**
```xml
<dependency>
    <groupId>com.github.sereneant.segystream</groupId>
    <artifactId>core_2.12</artifactId>
    <version>0.1.0</version>
</dependency>
```

**Gradle**
```groovy
dependencies {
  compile group: 'com.github.sereneant.segystream', name: 'core_2.12', version: '0.1.0'
}
```

Streaming implementation is based on [Akka Streams](https://doc.akka.io/docs/akka/2.5/stream/index.html).

**Scala**

Setup streams:
```Scala
  implicit val system: ActorSystem = ActorSystem("segystream-examples")
  implicit val mat: ActorMaterializer = ActorMaterializer()
```

Construct Stream blueprint from Seg-Y file or another byte sources (S3, HDFS, etc).
```scala
  val segySource: Source[SegyPart, Future[SegyHeaders]] = fileSource.viaMat(SegyFlow())(Keep.right)
```
Full spectre of [Alpakka Connectors](https://developer.lightbend.com/docs/alpakka/current/) can be used for streaming from different sources / to different sinks.

Run the flow, make actions/transformations:
```scala
  val done: Future[Done] = segySource
    .map {
      case th: TraceHeader => println(s"Trace Header: ${th.traceSequenceNumberWithinLine}")
      case td: TraceDataChunk => println(s"Trace Data Chunk: length=${td.length}")
      case _ => // NoOp
    }
    .toMat(Sink.ignore)(Keep.right) // wait for the Sink to complete
    .run()
```

Wait for stream termination and print the stats:
```scala
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  done.onComplete { _ =>
    system.terminate()
    println("Stream completed")
  }
```
**Java**

See [PrintDebugInfo](examples/src/main/java/com.github.sereneant.segystrem.examples/PrintDebugInfo.java) example program.

## Configuration
Stream of Seg-Y data in traces is split into chunks of configurable length, default is 1024 bytes.

Custom configuration can be passed to `SegyFlow` constructor:
```scala
val segyFlow = new SegyFlow(SegyConfig(
  charset: Charset = Charset.forName("CP037"), //textual data charset
  dataChunkSize: Int = 1024 //bytes
))
```

### Building from sources
```bash
sbt package
```

### Publish to local repo repository
**Ivy**
```bash
sbt publishLocal
```
**Maven**
```bash
sbt publishM2
```

### Running tests
```bash
sbt test
```

### Running benchmarks
_TBD_

### Running examples
Examples are located in [examples](examples) folder.
```bash
sbt "examples/runMain com.github.sereneant.segystrem.examples.CollectSegyStats SegY_file_name.segy"
```

## Known Issues
* Parser does not support variable extended text headers.
* Parser does not support Data Sample Format Code 4 (4-byte fixed-point with gain, obsolete).

## Contributing
Any contributions are welcome!
It can be done by creating issues and pull requests on a [project GitHub page](https://github.com/SereneAnt/segystream).

Please keep code clean (whatever it means for you) and comply with coding style standards:
* [Scala style guide](https://docs.scala-lang.org/style)
* [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

Please keep a [CHANGELOG.md](CHANGELOG.md) file in actual state;
the format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/).

## Versioning
[SemVer](http://semver.org/) is used as versioning standard.
For the version references, see the [git tags](https://github.com/SereneAnt/segystream/tags).

## License
Licensed under the MIT License - see the [LICENSE](LICENSE) file.

## Acknowledgments
* Inspired by [Reactive Manifesto](https://www.reactivemanifesto.org)
* Thanks to [Mikhail Aksenov](https://github.com/thecoldwine) for [sigrun](https://github.com/thecoldwine/sigrun), used as a good starter in Seg-Y parsing.
* Thanks to [Andriy Plokhotnyuk](https://github.com/plokhotnyuk) for his [jsoniter-scala](https://github.com/plokhotnyuk/jsoniter-scala) as an example of technical excellence and well shaped scala project, where build configuration and project structure were borrowed from.

## Alternative noteworthy implementations
All references are given in alphabetical order.
#### Java
* https://github.com/cloudera/seismichadoop
* https://github.com/dhale/idh/tree/master/bench/src/segy
* https://github.com/thecoldwine/sigrun
#### Python
* https://github.com/obspy/obspy
* https://github.com/sixty-north/segpy
* https://github.com/Statoil/segyio
