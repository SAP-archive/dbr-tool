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

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Keep}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.util.ByteString
import better.files.File
import com.hybris.core.dbr.BaseCoreTest
import com.hybris.core.dbr.config.RestoreTypeConfig
import com.hybris.core.dbr.document.DocumentBackupClient
import com.hybris.core.dbr.exceptions.RestoreException
import com.hybris.core.dbr.model.RestoreTypeData

import scala.concurrent.Future

class RestoreStreamTest extends BaseCoreTest with RestoreStream {

  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  "RestoreStream" should {

    "add documents from file" in {

      val testDir = File.newTemporaryDirectory()
      val fileName1 = randomName
      val fileName2 = randomName

      testDir / fileName1 overwrite
        """
          |[
          | { "file1" : 1 },
          | { "file1" : 2 }
          |]
        """.stripMargin

      testDir / fileName2 overwrite
        """
          | [
          |   { "file2" : 1 },
          |   { "file2" : 2 },
          |   { "file2" : 3 }
          | ]
        """.stripMargin

      val (source, sink) = TestSource.probe[RestoreTypeConfig]
        .via(addDocuments(testDir.pathAsString))
        .toMat(TestSink.probe[RestoreTypeData])(Keep.both)
        .run()

      sink.request(2)

      source.sendNext(RestoreTypeConfig("client", "tenant1", "type1", fileName1))
      source.sendNext(RestoreTypeConfig("client", "tenant1", "type2", fileName2))
      source.sendComplete()

      sink.expectNextPF {
        case RestoreTypeData("client", "tenant1", "type1", documents) ⇒
          documents
            .runWith(TestSink.probe[ByteString])
            .request(1)
            .expectNextPF {
              case documents: ByteString ⇒
                documents.utf8String must include("""{ "file1" : 1 }""")
                documents.utf8String must include("""{ "file1" : 2 }""")
            }
      }

      sink.expectNextPF {
        case RestoreTypeData("client", "tenant1", "type2", documents) ⇒
          documents
            .runWith(TestSink.probe[ByteString])
            .request(1)
            .expectNextPF {
              case documents: ByteString ⇒
                documents.utf8String must include("""{ "file2" : 1 }""")
                documents.utf8String must include("""{ "file2" : 2 }""")
                documents.utf8String must include("""{ "file2" : 3 }""")
            }

      }

      sink.expectComplete()
    }

    "fail when type's file not found" in {

      val testDir = File.newTemporaryDirectory()
      val fileName = randomName

      val (source, sink) = TestSource.probe[RestoreTypeConfig]
        .via(addDocuments(testDir.pathAsString))
        .toMat(TestSink.probe[RestoreTypeData])(Keep.both)
        .run()

      sink.request(10)

      source.sendNext(RestoreTypeConfig("client", "tenant1", "type1", fileName))
      source.sendComplete()

      val error = sink.expectError()
      error mustBe a[RestoreException]
      error.getMessage must include(fileName)
      error.getMessage must include("not found")
    }

    "insert documents" in {

      val documentBackupClient = stub[DocumentBackupClient]
      val fileSource1 = FileIO.fromPath(Paths.get("a"))
      val fileSource2 = FileIO.fromPath(Paths.get("b"))

      (documentBackupClient.insertRawDocuments _)
        .when("client", "tenant1", "type1", fileSource1)
        .returns(Future.successful(1))
      (documentBackupClient.insertRawDocuments _)
        .when("client", "tenant1", "type1", fileSource2)
        .returns(Future.successful(1))

      val (source, sink) = TestSource.probe[RestoreTypeData]
        .via(insertDocuments(documentBackupClient))
        .toMat(TestSink.probe[Int])(Keep.both)
        .run()

      sink.request(10)

      source.sendNext(RestoreTypeData("client", "tenant1", "type1", fileSource1))
      source.sendNext(RestoreTypeData("client", "tenant1", "type1", fileSource2))
      source.sendComplete()

      sink.expectNextUnordered(1, 1)
      sink.expectComplete()
    }

    "fail when inserting document fails" in {

      val documentBackupClient = stub[DocumentBackupClient]
      val fileSource1 = FileIO.fromPath(Paths.get("a"))

      (documentBackupClient.insertRawDocuments _)
        .when("client", "tenant1", "type1", fileSource1)
        .returns(Future.failed(new RuntimeException("some error")))

      val (source, sink) = TestSource.probe[RestoreTypeData]
        .via(insertDocuments(documentBackupClient))
        .toMat(TestSink.probe[Int])(Keep.both)
        .run()

      sink.request(10)

      source.sendNext(RestoreTypeData("client", "tenant1", "type1", fileSource1))
      source.sendComplete()

      val error = sink.expectError()
      error mustBe a[RestoreException]
    }
  }
}
