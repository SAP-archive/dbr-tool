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
package com.hybris.core.dbr.restore

import java.nio.file.Paths

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Keep, Source}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.util.ByteString
import better.files.File
import com.hybris.core.dbr.BaseCoreTest
import com.hybris.core.dbr.config.RestoreTypeDefinition
import com.hybris.core.dbr.document.{DocumentBackupClient, DocumentServiceClient, InsertResult}
import com.hybris.core.dbr.exceptions.RestoreException
import com.hybris.core.dbr.model.IndexDefinition
import io.circe.Json

import scala.concurrent.{Await, Future}

class RestoreStreamTest extends BaseCoreTest with RestoreStream {

  implicit val materializer = ActorMaterializer()

  implicit val ec = system.dispatcher

  "RestoreStream" should {

    "add documents from file" in {

      val testDir = File.newTemporaryDirectory()
      val fileName = randomName

      testDir / fileName overwrite
        """
          |[
          | { "file1" : 1 },
          | { "file1" : 2 }
          |]
        """.stripMargin

      val indexDefinition = IndexDefinition(Json.fromString("""{"file1": 1}"""), Json.fromString("""{"name": "test"}"""))
      val stream =
        configToFileChunksSource(testDir.pathAsString, RestoreTypeDefinition("client", "tenant1", "type1", fileName, Some(List(indexDefinition))))
          .toMat(TestSink.probe[Source[ByteString, _]])(Keep.right)
          .run()

      stream.request(2)

      val documents = stream.expectNext()

      val documentsProbe = documents.runWith(TestSink.probe[ByteString])
      documentsProbe.request(2)
      documentsProbe.expectNext().utf8String must include("""{ "file1" : 1 }""")
      documentsProbe.expectNext().utf8String must include("""{ "file1" : 2 }""")
      documentsProbe.expectComplete()

      stream.expectComplete()
    }

    "add documents from file grouping by 1000" in {

      import scala.concurrent.duration.DurationInt

      val testDir = File.newTemporaryDirectory()
      val fileName1 = randomName

      val result = Await.result(generateJsons(2001), 10 seconds)

      testDir / fileName1 overwrite
        s"""
           |[
           | $result
           |]
            """.stripMargin

      val indexDefinition = IndexDefinition(Json.fromString("""{"file1": 1}"""), Json.fromString("""{"name": "test"}"""))
      val stream =
        configToFileChunksSource(testDir.pathAsString, RestoreTypeDefinition("client", "tenant1", "type1", fileName1, Some(List(indexDefinition))))
          .toMat(TestSink.probe[Source[ByteString, _]])(Keep.right)
          .run()

      stream.request(3)

      stream.expectNext().runFold(0)((acc, _) ⇒ acc + 1).futureValue mustBe 1000
      stream.expectNext().runFold(0)((acc, _) ⇒ acc + 1).futureValue mustBe 1000
      stream.expectNext().runFold(0)((acc, _) ⇒ acc + 1).futureValue mustBe 1
      stream.expectComplete()
    }

    "fail when type's file not found" in {

      val testDir = File.newTemporaryDirectory()
      val fileName = randomName

      val indexDefinition = IndexDefinition(Json.fromString("""{"file1": 1}"""), Json.fromString("""{"name": "test"}"""))
      val stream =
        configToFileChunksSource(testDir.pathAsString, RestoreTypeDefinition("client", "tenant1", "type1", fileName, Some(List(indexDefinition))))
          .toMat(TestSink.probe[Source[ByteString, _]])(Keep.right)
          .run()

      stream.request(10)

      val error = stream.expectError()
      error mustBe a[RestoreException]
      error.getMessage must include(fileName)
      error.getMessage must include("not found")
    }

    "insert documents" in {

      val documentBackupClient = stub[DocumentBackupClient]
      val fileSource1 = FileIO.fromPath(Paths.get("a"))
      val fileSource2 = FileIO.fromPath(Paths.get("b"))

      (documentBackupClient.insertDocuments _)
        .when("client", "tenant1", "type1", fileSource1)
        .returns(Future.successful(InsertResult(1, 1, 0)))
      (documentBackupClient.insertDocuments _)
        .when("client", "tenant1", "type1", fileSource2)
        .returns(Future.successful(InsertResult(1, 1, 0)))

      val indexDefinition = IndexDefinition(Json.fromString("""{"file1": 1}"""), Json.fromString("""{"name": "test"}"""))
      val rdc = RestoreTypeDefinition("client", "tenant1", "type1", "a", Some(List(indexDefinition)))

      val (source, sink) = TestSource.probe[Source[ByteString, _]]
        .via(insertDocuments(documentBackupClient, rdc))
        .toMat(TestSink.probe[InsertResult])(Keep.both)
        .run()

      sink.request(10)

      source
        .sendNext(fileSource1)
        .sendNext(fileSource2)
        .sendComplete()

      sink.expectNextUnordered(InsertResult(1, 1, 0), InsertResult(1, 1, 0))
        .expectComplete()
    }

    "create indexes" in {
      // given
      val indexDefinition = IndexDefinition(Json.fromString("""{"file1": 1}"""), Json.fromString("""{"name": "test"}"""))

      val documentServiceClient = stub[DocumentServiceClient]
      (documentServiceClient.createIndex _)
        .when("client", "tenant1", "type1", indexDefinition)
        .returns(Future.successful(NotUsed))

      val rdc = RestoreTypeDefinition("client", "tenant1", "type1", "a", Some(List(indexDefinition)))

      val (source, sink) = TestSource.probe[RestoreTypeDefinition]
        .via(createIndexes(documentServiceClient))
        .toMat(TestSink.probe[RestoreTypeDefinition])(Keep.both)
        .run()

      // when
      sink.request(10)

      source
        .sendNext(rdc)
        .sendNext(rdc)
        .sendComplete()

      // then
      sink
        .expectNext(rdc)
        .expectNext(rdc)
        .expectComplete()
    }

    "fail when inserting document fails" in {

      val documentBackupClient = stub[DocumentBackupClient]
      val fileSource = FileIO.fromPath(Paths.get("a"))

      (documentBackupClient.insertDocuments _)
        .when("client", "tenant1", "type1", fileSource)
        .returns(Future.failed(new RuntimeException("some error")))

      val indexDefinition = IndexDefinition(Json.fromString("""{"file1": 1}"""), Json.fromString("""{"name": "test"}"""))
      val rdc = RestoreTypeDefinition("client", "tenant1", "type1", "a", Some(List(indexDefinition)))

      val (source, sink) = TestSource.probe[Source[ByteString, _]]
        .via(insertDocuments(documentBackupClient, rdc))
        .toMat(TestSink.probe[InsertResult])(Keep.both)
        .run()

      sink.request(10)

      source.
        sendNext(fileSource)
        .sendComplete()

      val error = sink.expectError()
      error mustBe a[RestoreException]
    }

    "aggregate results" in {

      //given
      val s = Source.repeat(InsertResult(1, 1, 0)).take(2001)

      val indexDefinition = IndexDefinition(Json.fromString("""{"file1": 1}"""), Json.fromString("""{"name": "test"}"""))
      val rdc = RestoreTypeDefinition("client", "tenant", "type", "a", Some(List(indexDefinition)))

      //when
      val stream = s.via(aggregateAndLogResults(rdc))
        .toMat(TestSink.probe[InsertResult])(Keep.right)
        .run()

      stream.request(10)

      //then
      stream
        .expectNext(InsertResult(2001, 2001, 0))
        .expectComplete()
    }
  }


  private def generateJsons(n: Long) =
    Source
      .repeat( """{a: "1"}""")
      .take(n)
      .runFold("")((acc, t) ⇒ acc ++ t)

}
