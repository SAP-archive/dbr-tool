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
package com.hybris.core.dbr.backup

import akka.Done
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import better.files.File
import com.hybris.core.dbr.BaseCoreTest
import com.hybris.core.dbr.config.FileConfig
import com.hybris.core.dbr.document.{DocumentBackupClient, DocumentServiceClient}
import com.hybris.core.dbr.model.ClientTenant
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Future

class BackupServiceTest extends BaseCoreTest with FileConfig {

  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  implicit val defaultPatience = PatienceConfig(timeout =  Span(5, Seconds), interval = Span(250, Millis))

  "BackupService" should {

    "backup data" in {

      // given
      val dstDir = File.newTemporaryDirectory()

      val type1Stream = Source(List("[", """{"type1":1}""", ",", """{"type1":2}""", "]")).map(ByteString(_))
      val type2Stream = Source(List("[", """{"type2":1}""", "]")).map(ByteString(_))

      val documentBackupClient = stub[DocumentBackupClient]
      val documentServiceClient = stub[DocumentServiceClient]
      (documentServiceClient.getTypes _).when("client", "tenant").returns(Future.successful(List("type1", "type2")))
      (documentBackupClient.getDocuments _).when("client", "tenant", "type1")
        .returns(Future.successful(type1Stream))
      (documentBackupClient.getDocuments _).when("client", "tenant", "type2")
        .returns(Future.successful(type2Stream))

      val backupService = new BackupService(documentBackupClient, documentServiceClient, dstDir.pathAsString, "backup.json")

      // when
      val result = backupService.runBackup(List(ClientTenant("client", "tenant", List()))).futureValue

      // then
      result mustBe Done

      val restoreConfig = readRestoreConfig(dstDir.pathAsString + "/backup.json").toEither.right.value
      restoreConfig.types must have size 2

      val rtc1 = restoreConfig.types
        .find(rtc => rtc.client == "client" && rtc.tenant == "tenant" && rtc.`type` == "type1").value
      rtc1.file must not be empty

      val rtc2 = restoreConfig.types
        .find(rtc => rtc.client == "client" && rtc.tenant == "tenant" && rtc.`type` == "type2").value
      rtc2.file must not be empty

      val file1Content = File(s"${dstDir.pathAsString}/${rtc1.file}").contentAsString
      file1Content mustBe """[{"type1":1},{"type1":2}]"""

      val file2Content = File(s"${dstDir.pathAsString}/${rtc2.file}").contentAsString
      file2Content mustBe """[{"type2":1}]"""

    }
  }

}
