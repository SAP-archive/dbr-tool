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
import io.circe.Decoder
import io.circe.parser._

object FileOps {

  case object Ready

  sealed class FileError(message: String) {
    def getMessage: String = message
  }

  case class GenericFileError(message: String) extends FileError(message)

  case class FileNotFoundError(path: String) extends FileError(s"File $path not found")

  case class FileParsingError(message: String) extends FileError(message)

  /**
   * Prepares empty directory.
   *
   * @param path path to directory
   * @return
   */
  def prepareEmptyDir(path: String): Xor[FileError, Ready.type] = {
    val dstDir = File(path)

    try {
      if (dstDir.exists && dstDir.isDirectory) {
        if (!dstDir.isEmpty) dstDir.clear()
        Ready.right
      } else if (dstDir.exists && dstDir.isRegularFile) {
        GenericFileError("Destination directory is a file.").left
      } else if (dstDir.notExists) {
        createDirectory(dstDir)
      } else {
        GenericFileError("Failed to prepare destination directory.").left
      }
    } catch {
      case e: Exception =>
        GenericFileError(s"Failed to prepare destination directory, error: ${e.getMessage}").left
    }
  }

  private def createDirectory(dir: File): Xor[FileError, Ready.type] = {
    try {
      dir.createDirectory()
      Ready.right
    } catch {
      case e: AccessDeniedException =>
        GenericFileError("Failed to prepare destination directory, access denied.").left
      case e: NoSuchFileException =>
        GenericFileError("Failed to prepare destination directory, path doesn't exist.").left
      case e: Throwable =>
        GenericFileError(s"Failed to prepare destination directory, error: ${e.getMessage}.").left
    }
  }

  /**
   * Reads content from file and decodes it to given type.
   *
   * @param path path to file
   * @param decoder decoder to decode content
   * @tparam T expected type of result
   * @return
   */
  def readFileAs[T](path: String)(implicit decoder: Decoder[T]): Xor[FileError, T] = {
    val file = File(path)

    if (file.exists) {
      parse(file.contentAsString)
        .flatMap(json => json.as[T])
        .leftMap(error => FileParsingError(s"Failed to read file '$path', unexpected content, error: " + error.getMessage))
    } else {
      FileNotFoundError(path).left
    }
  }
}
