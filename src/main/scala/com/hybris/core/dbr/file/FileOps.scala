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
