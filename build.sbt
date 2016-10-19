/*
* [y] hybris Platform
*
* Copyright (c) 2000-2016 hybris AG
* All rights reserved.
*
* This software is the confidential and proprietary information of hybris
* ("Confidential Information"). You shall not disclose such Confidential
* Information and shall use it only in accordance with the terms of the
* license agreement you entered into with hybris.
*/

name := "dbr"

version := "0.0.1"

scalaVersion := "2.11.8"

libraryDependencies ++= {
  val akkaVersion = "2.4.11"
  val circeVersion = "0.5.4"

  Seq(
    "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,

    // files
    "com.github.pathikrit" %% "better-files" % "2.16.0",

    // cmd parameters
    "com.github.scopt" %% "scopt" % "3.5.0",

    // circe
    "de.heikoseeberger" %% "akka-http-circe" % "1.10.1",
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,

    "org.typelevel" %% "cats" % "0.7.2",
    "com.ironcorelabs" %% "cats-scalatest" % "1.1.2" % "test",

    // configuration
    "com.typesafe" % "config" % "1.3.1",

    // logging
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "ch.qos.logback" % "logback-classic" % "1.1.7",

    // tests
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % "test",
    "org.scalatest" %% "scalatest" % "2.2.6" % "test"
      exclude("org.scala-lang", "scala-reflect"),
    "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"
      exclude("org.scala-lang", "scala-reflect")
  )
}

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-language:_",
  "-target:jvm-1.7",
  "-encoding", "UTF-8",
  "-language:postfixOps",
  "-Ywarn-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-inaccessible",
  "-Ywarn-infer-any",
  "-Ywarn-nullary-override",
  "-Ywarn-nullary-unit",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused",
  "-Ywarn-unused-import"
  // "-Ywarn-value-discard"
  //  "-Y"
)

enablePlugins(JavaAppPackaging)

packageName in Universal := name.value

target in Universal := file("artifact")

mappings in Universal in packageBin += file("USER_GUIDE.md") -> "README.md"
