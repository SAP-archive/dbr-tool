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
import com.hybris.core.dbr.file.FileOps.Ready
import com.hybris.core.dbr.model.InternalAppError

import scala.util.Random

class FileOpsTest extends BaseTest with FileOps {

  "FileOps" should {

    "prepare empty dir when it doesn't exist" in {
      // given
      val path = s"/tmp/$randomName"

      // when
      val result = prepareEmptyDir(path)

      // then
      result mustBe Ready.right

      // clean up
      cleanUp(path)
    }

    "prepare empty dir when it exists and it's empty" in {
      // given
      val path = s"/tmp/$randomName"
      File(path).createDirectory()

      // when
      val result = prepareEmptyDir(path)

      // then
      result mustBe Ready.right

      // clean up
      cleanUp(path)
    }

    "prepare empty dir when it exists and it's not empty" in {
      // given
      val path = s"/tmp/$randomName"
      File(path).createDirectory()
      File(s"$path/$randomName").touch(Instant.now())

      // when
      val result = prepareEmptyDir(path)

      // then
      result mustBe Ready.right

      // clean up
      cleanUp(path)
    }

    "fail to create empty dir when path doesn't exist" in {
      // given
      val path = s"/tmp/$randomName/$randomName"

      // when
      val result = prepareEmptyDir(path).toEither.left.value

      // then
      result mustBe a[InternalAppError]
    }

    "fail to create empty dir when path leads to file" in {
      val path = s"/tmp/$randomName"
      File(path).touch(Instant.now())

      // when
      val result = prepareEmptyDir(path).toEither.left.value

      // then
      result mustBe a[InternalAppError]

      // clean up
      cleanUp(path)
    }

  }

  private def randomName = Random.alphanumeric take 10 mkString

  private def cleanUp(path: String) = File(path).delete()
}
