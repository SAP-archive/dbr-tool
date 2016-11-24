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
  def prepareEmptyDir(path: String): Either[FileError, Ready.type] = {
    val dstDir = File(path)

    try {
      if (dstDir.exists && dstDir.isDirectory) {
        if (!dstDir.isEmpty) dstDir.clear()
        Right(Ready)
      } else if (dstDir.exists && dstDir.isRegularFile) {
        Left(GenericFileError("Destination directory is a file."))
      } else if (dstDir.notExists) {
        createDirectory(dstDir)
      } else {
        Left(GenericFileError("Failed to prepare destination directory."))
      }
    } catch {
      case e: Exception =>
        Left(GenericFileError(s"Failed to prepare destination directory, error: ${e.getMessage}"))
    }
  }

  private def createDirectory(dir: File): Either[FileError, Ready.type] = {
    try {
      dir.createDirectory()
      Right(Ready)
    } catch {
      case e: AccessDeniedException =>
        Left(GenericFileError("Failed to prepare destination directory, access denied."))
      case e: NoSuchFileException =>
        Left(GenericFileError("Failed to prepare destination directory, path doesn't exist."))
      case e: Throwable =>
        Left(GenericFileError(s"Failed to prepare destination directory, error: ${e.getMessage}."))
    }
  }

  /**
   * Reads content from file and decodes it to given type.
   *
   * @param path    path to file
   * @param decoder decoder to decode content
   * @tparam T expected type of result
   * @return
   */
  def readFileAs[T](path: String)(implicit decoder: Decoder[T]): Either[FileError, T] = {
    val file = File(path)

    if (file.exists) {
      decode[T](file.contentAsString)
        .leftMap(error => FileParsingError(s"Failed to read file '$path', unexpected content, error: " + error.getMessage))
    } else {
      Left(FileNotFoundError(path))
    }
  }
}
