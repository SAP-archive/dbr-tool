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
import com.hybris.core.dbr.{BaseCoreTest, Main}
import org.scalatest.time.{Millis, Seconds, Span}

class DocumentBackupToolTest extends BaseCoreTest {

  implicit def defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(200, Millis))

  import system.dispatcher

  implicit val materializer = ActorMaterializer()

  val tenant = "framefrog"
  val client = "framefrog.mycomicsshop"
  val `type` = "dbrtest"

  /**
    * This is a suite of end-to-end tests.
    * In order to run it, export CLIENT_ID and CLIENT_SECRET for framefrog.mycomicsshop at first.
    * Please keep this test ignored.
    */
  "Document Backup Restore tool" ignore {

    "backup data from US and restore in EU" in {
      // Set up
      val clientId = sys.env("CLIENT_ID")
      val clientSecret = sys.env("CLIENT_SECRET")

      val usClient = DocumentServiceClient("api.us.yaas.io", tenant, client, `type`, clientId, clientSecret)
      val euClient = DocumentServiceClient("api.eu.yaas.io", tenant, client, `type`, clientId, clientSecret)

      // Create index and insert document to US
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

      val indexDefinition =
        """
          | {
          |   "keys": {
          |     "i": 1
          |   },
          |   "options": {
          |     "name": "dbrTestIndex"
          |   }
          | }
        """.stripMargin

      usClient.createIndex(indexDefinition).futureValue
      val usDocumentId = usClient.insertDocument(document).futureValue

      // Prepare config
      val (testDir, backupDir, configFileName) = prepareConfig(tenant, `type`)

      // Backup from US
      Main.main(Array("backup", "--env=us-prod", "--client=" + client, s"--out=$backupDir", s"--config=${testDir / configFileName}"))
      Thread.sleep(3000)

      // Restore to EU
      val backupTimestampDir = backupDir.list.toList.head
      Main.main(Array("restore", "--env=eu", s"--dir=$backupTimestampDir"))
      Thread.sleep(3000)

      // Verify the document in EU
      val documentFromEu = euClient.getDocument(usDocumentId).futureValue
      val indexFromEuStatus = euClient.getIndex("dbrTestIndex").futureValue

      documentFromEu must include(usDocumentId)
      indexFromEuStatus mustBe 200

      // Cleanup
      usClient.deleteType.futureValue
      euClient.deleteType.futureValue
    }

    "create new dir with timestamp for backup" in {
      // given
      val (testDir, backupDir, configFileName) = prepareConfig(tenant, `type`)

      // when
      Main.main(Array("backup", "--env=us-prod", "--client=" + client, s"--out=$backupDir", s"--config=${testDir / configFileName}"))
      Thread.sleep(1000)

      // then
      val backupDirFiles = backupDir.list.toList
      backupDirFiles.size mustBe 1
      backupDirFiles.head.name must startWith("backup-")
    }

    "not clear backup directory" in {
      // given
      val (testDir, backupDir, configFileName) = prepareConfig(tenant, `type`)

      backupDir / "myFile.txt" overwrite "My file!"

      // when
      Main.main(Array("backup", "--env=us-prod", "--client=" + client, s"--out=$backupDir", s"--config=${testDir / configFileName}"))
      Thread.sleep(10)

      // then
      (backupDir / "myFile.txt" exists) mustBe true
    }
  }

  private def prepareConfig(tenant: String, `type`: String) = {
    val testDir = File.newTemporaryDirectory()
    val backupDir = testDir / "backup"
    val configFileName = randomName

    testDir / configFileName overwrite
      s"""
         |{
         |  "tenants" : [
         |    {
         |      "tenant" : "$tenant",
         |      "types" : ["${`type`}"]
         |    }
         |  ]
         |}
         |
        """.stripMargin

    backupDir createDirectory

    (testDir, backupDir, configFileName)
  }

}
