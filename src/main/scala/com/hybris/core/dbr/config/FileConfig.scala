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
package com.hybris.core.dbr.config

import cats.implicits._
import com.hybris.core.dbr.config.FileConfig._
import com.hybris.core.dbr.file.FileOps._
import com.hybris.core.dbr.model.InternalAppError
import io.circe._
import io.circe.generic.semiauto._

/**
  * Functions to read configuration from user provided JSONs.
  */
trait FileConfig {

  /**
    * Reads configuration for backup operation.
    *
    * @param path command line configuration
    * @return backup configuration
    */
  def readBackupConfig(path: String): Either[InternalAppError, BackupConfig] = {
    readFileAs[BackupConfig](path)
      .leftMap(error => convertToInternalAppError(error, path))

  }

  /**
    * Reads configuration for restorer operation
    *
    * @param path command line configuration
    * @return restore configuration
    */
  def readRestoreConfig(path: String): Either[InternalAppError, RestoreConfig] = {
    readFileAs[List[RestoreTypeConfig]](path)
      .map(types => RestoreConfig(types))
      .leftMap(error => convertToInternalAppError(error, path))
  }

  private def convertToInternalAppError(fileError: FileError, path: String): InternalAppError = {
    fileError match {
      case FileNotFoundError(_) =>
        InternalAppError(s"Failed to read configuration from '$path', file not found")

      case FileParsingError(message) =>
        InternalAppError(s"Failed to parse configuration from '$path', error: $message")

      case other =>
        InternalAppError(s"Failed to read configuration from '$path', error: ${other.getMessage}")
    }
  }

}

object FileConfig {
  implicit val backupConfigDecoder: Decoder[BackupConfig] = deriveDecoder

  implicit val backupTenantConfigDecoder: Decoder[BackupTenantConfig] = deriveDecoder

  implicit val restoreTypeConfig: Decoder[RestoreTypeConfig] = deriveDecoder
}
