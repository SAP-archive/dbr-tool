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
import com.hybris.core.dbr.model.{ClientTenant, IndexDefinition}
import io.circe.Json
import io.circe.parser.parse
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.{ExecutionContextExecutor, Future}

class BackupServiceTest extends BaseCoreTest with FileConfig {

  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(250, Millis))

  "BackupService" should {

    "backup data with index" in {

      // given
      val dstDir = File.newTemporaryDirectory()

      val type1Stream = Source(List("[", """{"type1":1}""", ",", """{"type1":2}""", "]")).map(ByteString(_))
      val type2Stream = Source(List("[", """{"type2":1}""", "]")).map(ByteString(_))

      val documentBackupClient = stub[DocumentBackupClient]
      val documentServiceClient = stub[DocumentServiceClient]
      (documentServiceClient.getTypes _).when("client", "tenant").returns(Future.successful(Set("type1", "type2")))
      (documentBackupClient.getDocuments _).when("client", "tenant", "type1")
        .returns(Future.successful(type1Stream))
      (documentBackupClient.getDocuments _).when("client", "tenant", "type2")
        .returns(Future.successful(type2Stream))

      val keys = parse("""{ "field1": 1 }""").getOrElse(Json.Null)
      val options = parse("""{ "name":"field1Index" }""").getOrElse(Json.Null)
      val indexDefinition = IndexDefinition(keys, options)

      (documentServiceClient.getIndexes _).when("client", "tenant", "type1").returns(Future.successful(List(indexDefinition)))
      (documentServiceClient.getIndexes _).when("client", "tenant", "type2").returns(Future.successful(List(indexDefinition)))

      val backupService = new BackupService(documentBackupClient, documentServiceClient, dstDir.pathAsString, "backup.json", false)

      // when
      val result = backupService.runBackup(List(ClientTenant("client", "tenant", Set()))).futureValue

      // then
      result mustBe Done

      val restoreConfig = readRestoreDefinition(dstDir.pathAsString + "/backup.json").right.value
      restoreConfig.definitions must have size 2

      val rtc1 = restoreConfig.definitions
        .find(rtc => rtc.client == "client" && rtc.tenant == "tenant" && rtc.`type` == "type1").value
      rtc1.file must not be empty
      rtc1.indexes.get.head.keys.noSpaces mustBe """{"field1":1}"""
      rtc1.indexes.get.head.options.noSpaces mustBe """{"name":"field1Index"}"""

      val rtc2 = restoreConfig.definitions
        .find(rtc => rtc.client == "client" && rtc.tenant == "tenant" && rtc.`type` == "type2").value
      rtc2.file must not be empty
      rtc2.indexes.get.head.keys.noSpaces mustBe """{"field1":1}"""
      rtc2.indexes.get.head.options.noSpaces mustBe """{"name":"field1Index"}"""

      val file1Content = File(s"${dstDir.pathAsString}/${rtc1.file}").contentAsString
      file1Content mustBe """[{"type1":1},{"type1":2}]"""

      val file2Content = File(s"${dstDir.pathAsString}/${rtc2.file}").contentAsString
      file2Content mustBe """[{"type2":1}]"""

    }

    "backup data without indexes" in {

      // given
      val dstDir = File.newTemporaryDirectory()

      val type1Stream = Source(List("[", """{"type1":1}""", ",", """{"type1":2}""", "]")).map(ByteString(_))
      val type2Stream = Source(List("[", """{"type2":1}""", "]")).map(ByteString(_))

      val documentBackupClient = stub[DocumentBackupClient]
      val documentServiceClient = stub[DocumentServiceClient]
      (documentServiceClient.getTypes _).when("client", "tenant").returns(Future.successful(Set("type1", "type2")))
      (documentBackupClient.getDocuments _).when("client", "tenant", "type1")
        .returns(Future.successful(type1Stream))
      (documentBackupClient.getDocuments _).when("client", "tenant", "type2")
        .returns(Future.successful(type2Stream))

      val backupService = new BackupService(documentBackupClient, documentServiceClient, dstDir.pathAsString, "backup.json", true)

      // when
      val result = backupService.runBackup(List(ClientTenant("client", "tenant", Set()))).futureValue

      // then
      result mustBe Done

      val restoreConfig = readRestoreDefinition(dstDir.pathAsString + "/backup.json").right.value
      restoreConfig.definitions must have size 2

      val rtc1 = restoreConfig.definitions
        .find(rtc => rtc.client == "client" && rtc.tenant == "tenant" && rtc.`type` == "type1").value
      rtc1.file must not be empty

      val rtc2 = restoreConfig.definitions
        .find(rtc => rtc.client == "client" && rtc.tenant == "tenant" && rtc.`type` == "type2").value
      rtc2.file must not be empty

      val file1Content = File(s"${dstDir.pathAsString}/${rtc1.file}").contentAsString
      file1Content mustBe """[{"type1":1},{"type1":2}]"""

      val file2Content = File(s"${dstDir.pathAsString}/${rtc2.file}").contentAsString
      file2Content mustBe """[{"type2":1}]"""

      (documentServiceClient.getIndexes _).verify(*, *, *).never()
    }
  }

}
