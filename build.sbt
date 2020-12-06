val Http4sVersion = "0.21.11"
val CirceVersion = "0.13.0"
val Specs2Version = "4.10.5"
val LogbackVersion = "1.2.3"
val CatsVersion = "2.2.0"
val KindProjectorVersion = "0.11.1"
val Slf4jVersion = "1.7.30"
val ScalaCheckVersion = "1.15.1"
val ScalaTestVersion = "3.2.3"
val ScalaTestPlusVersion = "3.2.2.0"
val GRAALVM_VERSION = "20.2.0"

organization := "com.shankarnakai"
name := "points"
version := "0.0.1-SNAPSHOT"
scalaVersion in ThisBuild := "2.13.4"
crossScalaVersions := Seq("2.12.12", "2.13.4")

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
  "org.http4s" %% "http4s-circe" % Http4sVersion,
  "org.http4s" %% "http4s-dsl" % Http4sVersion,
  "io.circe" %% "circe-core" % CirceVersion,
  "io.circe" %% "circe-generic" % CirceVersion,
  "io.circe" %% "circe-generic-extras" % CirceVersion,
  "io.circe" %% "circe-parser" % CirceVersion,
  "io.circe" %% "circe-literal" % CirceVersion,
  "org.specs2" %% "specs2-core" % Specs2Version % "test",
  "ch.qos.logback" % "logback-classic" % LogbackVersion,
  "org.scalameta" %% "svm-subs" % GRAALVM_VERSION % "compile-internal",
  "org.typelevel" %% "cats-core" % CatsVersion,
  "org.scalacheck" %% "scalacheck" % ScalaCheckVersion % Test,
  "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
  "org.scalamock" %% "scalamock" % "4.4.0" % Test,
  "org.scalatestplus" %% "scalacheck-1-14" % ScalaTestPlusVersion % Test
)

dependencyOverrides += "org.slf4j" % "slf4j-api" % Slf4jVersion

dependencyOverrides += "io.circe" %% "circe-java8" % "0.11.1" cross CrossVersion.full

addCompilerPlugin(
  "org.typelevel" %% "kind-projector" % KindProjectorVersion cross CrossVersion.full)
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

enablePlugins(ScalafmtPlugin, JavaAppPackaging)
