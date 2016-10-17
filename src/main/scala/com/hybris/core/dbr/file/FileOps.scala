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


import java.nio.file.{AccessDeniedException, NoSuchFileException}

import better.files.File
import cats.data.Xor
import cats.implicits._
import com.hybris.core.dbr.file.FileOps.Ready
import com.hybris.core.dbr.model.InternalAppError

trait FileOps {

  def prepareEmptyDir(path: String): Xor[InternalAppError, Ready.type] = {
    val dstDir = File(path)

    try {
      if (dstDir.exists && dstDir.isDirectory) {
        if (!dstDir.isEmpty) dstDir.clear()
        Ready.right
      } else if (dstDir.exists && dstDir.isRegularFile) {
        InternalAppError("Destination directory is a file.").left
      } else if (dstDir.notExists) {
        createDirectory(dstDir)
      } else {
        InternalAppError("Failed to prepare destination directory.").left
      }
    } catch {
      case e: Exception =>
        InternalAppError(s"Failed to prepare destination directory, error: ${e.getMessage}").left
    }
  }

  private def createDirectory(dir: File): Xor[InternalAppError, Ready.type] = {
    try {
      dir.createDirectory()
      Ready.right
    } catch {
      case e: AccessDeniedException =>
        InternalAppError("Failed to prepare destination directory, access denied.").left
      case e: NoSuchFileException =>
        InternalAppError("Failed to prepare destination directory, path doesn't exist.").left
      case e: Throwable =>
        InternalAppError(s"Failed to prepare destination directory, error: ${e.getMessage}.").left
    }
  }
}

object FileOps {
  case object Ready
}
