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
package endtoend

import akka.stream.ActorMaterializer
import better.files.File
import com.hybris.core.dbr.oauth.OAuthClient
import com.hybris.core.dbr.{BaseCoreTest, Main}
import org.scalatest.time.{Millis, Seconds, Span}

class DocumentBackupToolTest extends BaseCoreTest {

  implicit def defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(200, Millis))

  import system.dispatcher

  implicit val materializer = ActorMaterializer()

  val `type` = "dbrTest"

  /**
    * This is a suite of end-to-end tests.
    * In order to run it, export CLIENT_ID and CLIENT_SECRET for framefrog.mycomicsshop at first.
    * Please keep this test ignored.
    */
  "Document Backup Restore tool" ignore {

    "backup data from US and restore in EU" in {

      import DocumentServiceClient._

      // Set up
      val clientId = sys.env("CLIENT_ID")
      val clientSecret = sys.env("CLIENT_SECRET")

      val scopes = List("hybris.document_view hybris.document_manage")
      val usToken = new OAuthClient("https://api.us.yaas.io/hybris/oauth2/v1/token",
        clientId,
        clientSecret,
        scopes).getToken.futureValue

      val euToken = new OAuthClient("https://api.eu.yaas.io/hybris/oauth2/v1/token",
        clientId,
        clientSecret,
        scopes).getToken.futureValue

      // Insert a document
      val document =
        """{
          |	"i": 1,
          |	"f": 1.2,
          |	"a": [1, 2, 3],
          |	"s": "Hello World!",
          |	"n": {
          |		"s": "Hello World!"
          |	},
          |	"b": true
          |}""".stripMargin

      val usDocumentId = insertDocument("https://api.us.yaas.io/hybris/document/v1", usToken, "framefrog", "framefrog.mycomicsshop", `type`, document).futureValue

      // Prepare config file
      val testDir = File.newTemporaryDirectory()
      val backupDir = testDir / "backup"
      val configFileName = randomName

      testDir / configFileName overwrite
        s"""
           |{
           |  "tenants" : [
           |    {
           |      "tenant" : "framefrog",
           |      "types" : ["${`type`}"]
           |    }
           |  ]
           |}
           |
        """.stripMargin

      backupDir createDirectory

      // Backup from us-prod
      Main.main(Array("backup", "--env=us-prod", "--client=framefrog.mycomicsshop", s"--out=$backupDir", s"--config=${testDir / configFileName}"))
      Thread.sleep(3000)

      // Restore to eu
      val backupTimestampDir = backupDir.list.toList.head
      Main.main(Array("restore", "--env=eu", s"--dir=$backupTimestampDir"))
      Thread.sleep(3000)

      // Verify the document in EU
      val documentFromEu = getDocument("https://api.eu.yaas.io/hybris/document/v1", euToken, "framefrog", "framefrog.mycomicsshop", `type`, usDocumentId).futureValue

      documentFromEu must include(usDocumentId)

      // Cleanup
      deleteType("https://api.us.yaas.io/hybris/document/v1", usToken, "framefrog", "framefrog.mycomicsshop", `type`).futureValue
      deleteType("https://api.eu.yaas.io/hybris/document/v1", euToken, "framefrog", "framefrog.mycomicsshop", `type`).futureValue
    }

    "create new dir with timestamp for backup" in {
      // given
      val testDir = File.newTemporaryDirectory()
      val backupDir = testDir / "backup"

      val configFileName = randomName

      testDir / configFileName overwrite
        s"""
           |{
           |  "tenants" : [
           |    {
           |      "tenant" : "framefrog",
           |      "types" : ["${`type`}"]
           |    }
           |  ]
           |}
           |
        """.stripMargin

      backupDir createDirectory

      // when
      Main.main(Array("backup", "--env=us-prod", "--client=framefrog.mycomicsshop", s"--out=$backupDir", s"--config=${testDir / configFileName}"))
      Thread.sleep(1000)

      // then
      val backupDirFiles = backupDir.list.toList
      backupDirFiles.size mustBe 1
      backupDirFiles.head.name must startWith ("backup-")
    }

    "not clear backup directory" in {
      // given
      val testDir = File.newTemporaryDirectory()
      val backupDir = testDir / "backup"
      val configFileName = randomName

      testDir / configFileName overwrite
        s"""
           |{
           |  "tenants" : [
           |    {
           |      "tenant" : "framefrog",
           |      "types" : ["${`type`}"]
           |    }
           |  ]
           |}
           |
        """.stripMargin

      backupDir createDirectory

      backupDir / "myFile.txt" overwrite "My file!"

      // when
      Main.main(Array("backup", "--env=us-prod", "--client=framefrog.mycomicsshop", s"--out=$backupDir", s"--config=${testDir / configFileName}"))
      Thread.sleep(10)

      // then
      (backupDir / "myFile.txt" exists) mustBe true
    }
  }
}
