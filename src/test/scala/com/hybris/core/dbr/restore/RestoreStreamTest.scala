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

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import better.files.File
import com.hybris.core.dbr.BaseCoreTest
import com.hybris.core.dbr.config.RestoreTypeConfig
import com.hybris.core.dbr.document.DocumentServiceClient
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

      sink.request(10)

      source.sendNext(RestoreTypeConfig("client", "tenant1", "type1", fileName1))
      source.sendNext(RestoreTypeConfig("client", "tenant1", "type2", fileName2))
      source.sendComplete()

      sink.expectNext(RestoreTypeData("client", "tenant1", "type1", """{"file1":1}"""))
      sink.expectNext(RestoreTypeData("client", "tenant1", "type1", """{"file1":2}"""))
      sink.expectNext(RestoreTypeData("client", "tenant1", "type2", """{"file2":1}"""))
      sink.expectNext(RestoreTypeData("client", "tenant1", "type2", """{"file2":2}"""))
      sink.expectNext(RestoreTypeData("client", "tenant1", "type2", """{"file2":3}"""))
      sink.expectComplete()
    }

    "omit type's files without documents" in {

      val testDir = File.newTemporaryDirectory()
      val fileName = randomName

      testDir / fileName overwrite "[]"

      val (source, sink) = TestSource.probe[RestoreTypeConfig]
        .via(addDocuments(testDir.pathAsString))
        .toMat(TestSink.probe[RestoreTypeData])(Keep.both)
        .run()

      sink.request(10)

      source.sendNext(RestoreTypeConfig("client", "tenant1", "type1", fileName))
      source.sendComplete()

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

    "fail when type's file not an array of documents" in {

      val testDir = File.newTemporaryDirectory()
      val fileName = randomName

      testDir / fileName overwrite "not a json"

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
    }

    "insert documents" in {

      val documentServiceClient = stub[DocumentServiceClient]

      (documentServiceClient.insertRawDocument _)
        .when("client", "tenant1", "type1", """{"file1":1}""")
        .returns(Future.successful("id1"))
      (documentServiceClient.insertRawDocument _)
        .when("client", "tenant1", "type1", """{"file1":2}""")
        .returns(Future.successful("id2"))

      val (source, sink) = TestSource.probe[RestoreTypeData]
        .via(insertDocuments(documentServiceClient))
        .toMat(TestSink.probe[String])(Keep.both)
        .run()

      sink.request(10)

      source.sendNext(RestoreTypeData("client", "tenant1", "type1", """{"file1":1}"""))
      source.sendNext(RestoreTypeData("client", "tenant1", "type1", """{"file1":2}"""))
      source.sendComplete()

      sink.expectNextUnordered("id1", "id2")
      sink.expectComplete()
    }

    "fail when inserting document fails" in {

      val documentServiceClient = stub[DocumentServiceClient]

      (documentServiceClient.insertRawDocument _)
        .when("client", "tenant1", "type1", """{"file1":1}""")
        .returns(Future.failed(new RuntimeException("some error")))

      val (source, sink) = TestSource.probe[RestoreTypeData]
        .via(insertDocuments(documentServiceClient))
        .toMat(TestSink.probe[String])(Keep.both)
        .run()

      sink.request(10)

      source.sendNext(RestoreTypeData("client", "tenant1", "type1", """{"file1":1}"""))
      source.sendComplete()

      val error = sink.expectError()
      error mustBe a[RestoreException]
    }
  }


}
