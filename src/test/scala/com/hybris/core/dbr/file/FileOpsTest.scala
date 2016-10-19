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

import java.time.Instant

import better.files.File
import cats.implicits._
import com.hybris.core.dbr.BaseTest
import com.hybris.core.dbr.file.FileOps._
import io.circe.Decoder
import io.circe.generic.semiauto._

class FileOpsTest extends BaseTest {

  private case class TestPerson(name: String, age: Int)

  private implicit val testPersonDecoder: Decoder[TestPerson] = deriveDecoder

  "FileOps" when {

    "preparing empty directory" should {

      "create empty directory when it doesn't exist" in {
        // given
        val path = s"${File.temp.pathAsString}/$randomName"

        // when
        val result = prepareEmptyDir(path)

        // then
        result mustBe Ready.right

        val resultDir = File(path)
        resultDir.exists mustBe true
        resultDir.isEmpty mustBe true
      }

      "accept directory when it exists and it's empty" in {
        // given
        val tmpDir = File.newTemporaryDirectory()

        // when
        val result = prepareEmptyDir(tmpDir.pathAsString)

        // then
        result mustBe Ready.right

        tmpDir.exists mustBe true
        tmpDir.isEmpty mustBe true
      }

      "clear directory when it exists and it's not empty" in {
        // given
        val tmpDir = File.newTemporaryDirectory()
        File(s"${tmpDir.pathAsString}/$randomName").touch(Instant.now())

        // when
        val result = prepareEmptyDir(tmpDir.pathAsString)

        // then
        result mustBe Ready.right

        tmpDir.exists mustBe true
        tmpDir.isEmpty mustBe true
      }

      "fail to create empty dir when path doesn't exist" in {
        // given
        val path = s"/${File.temp.pathAsString}/$randomName/$randomName"

        // when
        val result = prepareEmptyDir(path).toEither.left.value

        // then
        result mustBe a[GenericFileError]
      }

      "fail to create empty dir when path leads to file" in {
        val path = s"/${File.temp.pathAsString}/$randomName"
        File(path).touch(Instant.now())

        // when
        val result = prepareEmptyDir(path).toEither.left.value

        // then
        result mustBe a[GenericFileError]

        // clean up
        cleanUp(path)
      }
    }

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

        val testPerson = readFileAs[TestPerson](file.pathAsString).toEither.right.value

        testPerson mustBe TestPerson("John", 30)
      }

      "return error if file doesn't exist" in {
        val file = File.temp / randomName

        val result = readFileAs[TestPerson](file.pathAsString).toEither.left.value

        result mustBe FileNotFoundError(file.pathAsString)
      }

      "return error when file contains wrong JSON" in {
        val file = File.newTemporaryFile()
        file.overwrite("not a json")

        val result = readFileAs(file.pathAsString).toEither.left.value

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

        val result = readFileAs(file.pathAsString).toEither.left.value

        result mustBe a[FileParsingError]
      }
    }
  }

  private def cleanUp(path: String) = File(path).delete()
}
