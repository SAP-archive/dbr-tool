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

import akka.Done
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import better.files.File
import com.hybris.core.dbr.BaseCoreTest
import com.hybris.core.dbr.config.RestoreTypeDefinition
import com.hybris.core.dbr.document.{DocumentBackupClient, DocumentServiceClient, InsertResult}
import io.circe.Json
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class RestoreServiceTest extends BaseCoreTest {

  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(250, Millis))

  "RestoreService" should {

    "restore data without index creation" in {
      // given
      val types = List(
        RestoreTypeDefinition("client", "tenant", "type1", "file1.json", Some(List(Json.fromString("""{keys: {"a1": 1}}""")))),
        RestoreTypeDefinition("client", "tenant", "type2", "file2.json", Some(List(Json.fromString("""{keys: {"a3": 1}}"""))))
      )

      val restoreDir = File.newTemporaryDirectory()
      restoreDir / "file1.json" overwrite """[{"a1":1},{"a2":2}]"""
      restoreDir / "file2.json" overwrite """[{"a3":1}]"""

      val documentBackupClient = mock[DocumentBackupClient]
      val documentServiceClient = stub[DocumentServiceClient]

      //@formatter:off
      (documentBackupClient.insertDocuments _)
        .expects(where { (client: String, tenant: String, `type`: String, documents: Source[ByteString, _]) ⇒
            val result = Await.result(documents.runWith(Sink.fold("")((acc, t) ⇒ acc.concat(t.utf8String))), 1 second)

            client == "client" &&
            tenant == "tenant" &&
            `type` == "type1" &&
            result == """{"a1":1}{"a2":2}"""
          })
        .returns(Future.successful(InsertResult(1,1,0)))

      (documentBackupClient.insertDocuments _)
        .expects(where { (client: String, tenant: String, `type`: String, documents: Source[ByteString, _]) ⇒
            val result = Await.result(documents.runWith(Sink.fold("")((acc, t) ⇒ acc.concat(t.utf8String))), 1 second)

            client == "client" &&
            tenant == "tenant" &&
            `type` == "type2" &&
            result == """{"a3":1}"""
          })
        .returns(Future.successful(InsertResult(1,1,0)))
      //@formatter:on

      val restoreService = new RestoreService(documentBackupClient, documentServiceClient, restoreDir.pathAsString)

      // when
      val result = restoreService.restore(types, true).futureValue

      // then (+ mock expectations)
      result mustBe Done
      (documentServiceClient.createIndex _).verify(*, *, *, *).never
    }

    "restore data with index creation" in {
      // given
      val indexDefinition = Json.fromString("""{keys: {"a1": 1}}""")
      val types = List(RestoreTypeDefinition("client", "tenant", "type1", "file1.json", Some(List(indexDefinition))))

      val restoreDir = File.newTemporaryDirectory()
      restoreDir / "file1.json" overwrite """[{"a1":1},{"a2":2}]"""

      val documentBackupClient = mock[DocumentBackupClient]
      val documentServiceClient = mock[DocumentServiceClient]

      //@formatter:off
      (documentBackupClient.insertDocuments _)
        .expects(where { (client: String, tenant: String, `type`: String, documents: Source[ByteString, _]) ⇒
            val result = Await.result(documents.runWith(Sink.fold("")((acc, t) ⇒ acc.concat(t.utf8String))), 1 second)

            client == "client" &&
            tenant == "tenant" &&
            `type` == "type1" &&
            result == """{"a1":1}{"a2":2}"""
          })
        .returns(Future.successful(InsertResult(1,1,0)))

      (documentServiceClient.createIndex _)
        .expects("client", "tenant", "type1", indexDefinition.toString)
        .returns(Future.successful("hwdp"))
      //@formatter:on

      val restoreService = new RestoreService(documentBackupClient, documentServiceClient, restoreDir.pathAsString)

      // when
      val result = restoreService.restore(types, false).futureValue

      // then (+ mock expectations)
      result mustBe Done
    }
  }
}
