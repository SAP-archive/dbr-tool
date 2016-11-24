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

  "Document Backup Restore tool" should {

    /**
      * This is an end-to-end test. In order to run it, export CLIENT_ID and CLIENT_SECRET for framefrog.mycomicsshop at first.
      * Please keep this test ignored.
      */
    "backup data from US and restore in EU" ignore {

      import DocumentServiceClient._

      // Set up
      val clientId = sys.env("CLIENT_ID")
      val clientSecret = sys.env("CLIENT_SECRET")

      val scopes = List("hybris.document_view hybris.document_manage")
      val usToken = new OAuthClient("https://api.yaas.io/hybris/oauth2/v1/token",
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

      val usDocumentId = insertDocument("https://api.yaas.io/hybris/document/v1", usToken, "framefrog", "framefrog.mycomicsshop", `type`, document).futureValue

      // Prepare config file
      val testDir = File.newTemporaryDirectory()
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

      // Backup from us-prod
      Main.main(Array("backup", "--env=us-prod", "--client=framefrog.mycomicsshop", s"--out=$testDir", s"--config=${testDir / configFileName}"))
      Thread.sleep(5000)

      // Restore to eu
      Main.main(Array("restore", "--env=eu", "--client=framefrog.mycomicsshop", s"--dir=$testDir"))
      Thread.sleep(5000)

      // Verify the document in EU
      val documentFromEu = getDocument("https://api.eu.yaas.io/hybris/document/v1", euToken, "framefrog", "framefrog.mycomicsshop", `type`, usDocumentId).futureValue

      documentFromEu must include(usDocumentId)

      // Cleanup
      deleteType("https://api.yaas.io/hybris/document/v1", usToken, "framefrog", "framefrog.mycomicsshop", `type`).futureValue
      deleteType("https://api.eu.yaas.io/hybris/document/v1", euToken, "framefrog", "framefrog.mycomicsshop", `type`).futureValue
    }
  }
}
