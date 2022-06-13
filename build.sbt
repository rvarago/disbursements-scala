ThisBuild / organization := "com.rvarago"
ThisBuild / scalaVersion := "3.1.0"

lazy val root = (project in file(".")).settings(
  name := "disbursements",
  libraryDependencies ++= dependencies ++ testDependencies
)

lazy val dependencies =
  Seq(
    "io.circe" %% "circe-generic" % "0.14.1",
    "org.typelevel" %% "squants" % "1.8.3",
    "ch.qos.logback" % "logback-classic" % "1.2.10",
    "com.github.tototoshi" %% "scala-csv" % "1.3.10",
    "is.cir" %% "ciris" % "2.3.2"
  ) ++
    Seq(
      "org.typelevel" %% "cats-effect",
      "org.typelevel" %% "cats-effect-kernel",
      "org.typelevel" %% "cats-effect-std"
    ).map(_ % "3.3.11") ++
    Seq(
      "org.http4s" %% "http4s-ember-server",
      "org.http4s" %% "http4s-circe",
      "org.http4s" %% "http4s-dsl"
    ).map(_ % "0.23.11") ++
    Seq(
      "co.fs2" %% "fs2-core",
      "co.fs2" %% "fs2-io"
    ).map(_ % "3.2.7")

lazy val testDependencies =
  Seq(
    "org.typelevel" %% "munit-cats-effect-3" % "1.0.7",
    "org.scalameta" %% "munit-scalacheck" % "0.7.29"
  ).map(_ % Test)
