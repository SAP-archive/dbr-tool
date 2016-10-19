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
import better.files.File
import com.hybris.core.dbr.BaseCoreTest
import com.hybris.core.dbr.config.RestoreTypeConfig
import com.hybris.core.dbr.document.DocumentServiceClient
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Future

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

      val documentServiceClient = mock[DocumentServiceClient]

      (documentServiceClient.insertRawDocument _)
        .expects("client", "tenant", "type1", """{"type1":1}""")
        .returns(Future.successful("id1"))
      (documentServiceClient.insertRawDocument _)
        .expects("client", "tenant", "type1", """{"type1":2}""")
        .returns(Future.successful("id2"))
      (documentServiceClient.insertRawDocument _)
        .expects("client", "tenant", "type2", """{"type2":1}""")
        .returns(Future.successful("id3"))

      val restoreService = new RestoreService(documentServiceClient, restoreDir.pathAsString)

      // when
      val result = restoreService.restore(types).futureValue

      // then (+ mock expectations)
      result mustBe Done
    }
  }
}
