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

import better.files.File
import com.hybris.core.dbr.BaseTest
import com.hybris.core.dbr.file.FileOps.{FileNotFoundError, FileParsingError}

class RestoreFileOpsTest extends BaseTest with RestoreFileOps {

  "RestoreFileOps" when {

    "reading document from file" should {

      "return list of documents" in {
        val file = File.newTemporaryFile()
        file.overwrite(
          """
            |[
            |  {"a" : 1},
            |  {"a" : 2},
            |  {"a" : 3}
            |]
          """.
            stripMargin)

        val result = readDocuments(file.pathAsString).toEither.right.value

        result must contain theSameElementsAs List( """{"a":1}""", """{"a":2}""", """{"a":3}""")
      }

      "return empty list if no documents in file" in {
        val file = File.newTemporaryFile()
        file.overwrite("[]")

        val result = readDocuments(file.pathAsString).toEither.right.value

        result mustBe empty
      }

      "return parse error when file contains wrong JSON" in {
        val file = File.newTemporaryFile()
        file.overwrite("not a json")

        val result = readDocuments(file.pathAsString).toEither.left.value

        result mustBe a[FileParsingError]
      }

      "return parse error when file doesn't exist" in {
        val file = File.temp / randomName

        val result = readDocuments(file.pathAsString).toEither.left.value

        result mustBe FileNotFoundError(file.pathAsString)
      }
    }
  }
}
