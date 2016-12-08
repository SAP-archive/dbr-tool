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
import com.hybris.core.dbr.config.RestoreTypeConfig
import com.hybris.core.dbr.document.{DocumentBackupClient, InsertResult}
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class RestoreServiceTest extends BaseCoreTest {

  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(250, Millis))

  "RestoreService" should {

    "restore data" in {

      // given
      val types = List(
        RestoreTypeConfig("client", "tenant", "type1", "file1.json"),
        RestoreTypeConfig("client", "tenant", "type2", "file2.json")
      )

      val restoreDir = File.newTemporaryDirectory()
      restoreDir / "file1.json" overwrite """[{"type1":1},{"type1":2}]"""
      restoreDir / "file2.json" overwrite """[{"type2":1}]"""

      val documentServiceClient = mock[DocumentBackupClient]

      //@formatter:off
      (documentServiceClient.insertDocuments _)
        .expects(where { (client: String, tenant: String, `type`: String, documents: Source[ByteString, _]) ⇒
            val result = Await.result(documents.runWith(Sink.head), 1 second)

            client == "client" &&
            tenant == "tenant" &&
            `type` == "type1" &&
            result == ByteString("""[{"type1":1},{"type1":2}]""")
          })
        .returns(Future.successful(InsertResult(1,1,0)))
      (documentServiceClient.insertDocuments _)
        .expects(where { (client: String, tenant: String, `type`: String, documents: Source[ByteString, _]) ⇒
            val result = Await.result(documents.runWith(Sink.head), 1 second)

            client == "client" &&
            tenant == "tenant" &&
            `type` == "type2" &&
            result == ByteString("""[{"type2":1}]""")
          })
        .returns(Future.successful(InsertResult(1,1,0)))
      //@formatter:on

      val restoreService = new RestoreService(documentServiceClient, restoreDir.pathAsString)

      // when
      val result = restoreService.restore(types).futureValue

      // then (+ mock expectations)
      result mustBe Done
    }
  }
}
