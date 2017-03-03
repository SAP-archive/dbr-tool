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
package com.hybris.core.dbr.file

import better.files.File
import com.hybris.core.dbr.BaseTest
import com.hybris.core.dbr.file.FileOps._
import io.circe.Decoder
import io.circe.generic.semiauto._

class FileOpsTest extends BaseTest {

  private case class TestPerson(name: String, age: Int)

  private implicit val testPersonDecoder: Decoder[TestPerson] = deriveDecoder

  "FileOps" when {

    "reading type from file" should {

      "return desired type" in {
        val file = File.newTemporaryFile()
        file.overwrite(
          """
            | {
            |   "name" : "John",
            |   "age" : 30
            | }
          """.stripMargin)

        val testPerson = readFileAs[TestPerson](file.pathAsString).right.value

        testPerson mustBe TestPerson("John", 30)
      }

      "return error if file doesn't exist" in {
        val file = File.temp / randomName

        val result = readFileAs[TestPerson](file.pathAsString).left.value

        result mustBe FileNotFoundError(file.pathAsString)
      }

      "return error when file contains wrong JSON" in {
        val file = File.newTemporaryFile()
        file.overwrite("not a json")

        val result = readFileAs(file.pathAsString).left.value

        result mustBe a[FileParsingError]
      }

      "return error when file contains unexpected JSON" in {
        val file = File.newTemporaryFile()
        file.overwrite(
          """
            | {
            |   "firstName" : "John",
            |   "numberOfYears" : 30
            | }
          """.stripMargin)

        val result = readFileAs(file.pathAsString).left.value

        result mustBe a[FileParsingError]
      }
    }
  }
}
