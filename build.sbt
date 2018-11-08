
lazy val commonSettings = Seq(
  name := "segystream",
  organization := "com.github.sereneant.segystream",
  scalaVersion := "2.12.7",
  publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath + "/.m2/repository")))
)

val akkaVersion = "2.5.18"
val scalatestVersion = "3.0.5"
val scalacheckVersion = "1.14.0"

lazy val segystream = project.in(file("."))
  .settings(commonSettings: _*)
  .aggregate(core, examples, benchmark)
  .settings(
    skip in publish := true,
  )

lazy val core = project
  .settings(commonSettings: _*)
  .settings(
    name := "segystream.core",
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.3.3",
      "com.chuusai" %% "shapeless" % "2.3.3",
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % scalatestVersion % Test,
      "org.scalacheck" %% "scalacheck" % scalacheckVersion % Test
    )
  )

lazy val examples = project
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "segystream.examples",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "org.hdrhistogram" % "HdrHistogram" % "2.1.10"
    ),
    skip in publish := true
  )

lazy val benchmark = project
  .enablePlugins(JmhPlugin)
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    name := "segystream.benchmark",
    libraryDependencies ++= Seq(
      "pl.project13.scala" % "sbt-jmh-extras" % "0.3.4",
      "org.scalatest" %% "scalatest" % scalatestVersion % Test
    ),
    skip in publish := true
  )
