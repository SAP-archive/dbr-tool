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

version := "0.2.1"

scalaVersion := "2.12.1"

libraryDependencies ++= {
  object Versions {
    val akka = "2.4.17"
    val akkaHttp = "10.0.4"
    val akkaHttpCirce = "1.12.0"
    val cats = "0.9.0"
    val circe = "0.7.0"
    val scalaLogging = "3.5.0"
    val typesafeConfig = "1.3.1"
    val betterFiles = "2.17.1"
    val scalaTest = "3.0.1"
    val logback = "1.2.1"
    val scopt = "3.5.0"
    val scalaMock = "3.5.0"
  }

  Seq(
    "com.typesafe.akka" %% "akka-http-core" % Versions.akkaHttp,
    "com.typesafe.akka" %% "akka-stream" % Versions.akka,
    "com.typesafe.akka" %% "akka-slf4j" % Versions.akka,

    // files
    "com.github.pathikrit" %% "better-files" % Versions.betterFiles,

    // cmd parameters
    "com.github.scopt" %% "scopt" % Versions.scopt,

    // circe
    "de.heikoseeberger" %% "akka-http-circe" % Versions.akkaHttpCirce,
    "io.circe" %% "circe-core" % Versions.circe,
    "io.circe" %% "circe-generic" % Versions.circe,
    "io.circe" %% "circe-parser" % Versions.circe,

    "org.typelevel" %% "cats" % Versions.cats,

    // configuration
    "com.typesafe" % "config" % Versions.typesafeConfig,

    // logging
    "com.typesafe.scala-logging" %% "scala-logging" % Versions.scalaLogging,
    "ch.qos.logback" % "logback-classic" % Versions.logback,

    // tests
    "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akka % "test",
    "org.scalatest" %% "scalatest" % Versions.scalaTest % "test"
      exclude("org.scala-lang", "scala-reflect"),
    "org.scalamock" %% "scalamock-scalatest-support" % Versions.scalaMock % "test"
      exclude("org.scala-lang", "scala-reflect")
  )
}

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-language:_",
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

enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq[BuildInfoKey](name, version)

buildInfoPackage := "com.hybris.core.dbr.config"

enablePlugins(JavaAppPackaging)

packageName in Universal := name.value + "-" + version.value

target in Universal := file("artifact")

mappings in Universal in packageBin += file("USER_GUIDE.md") -> "README.md"
