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
package com.hybris.core.dsb.config

import better.files._
import cats.data.Xor
import cats.implicits._
import com.hybris.core.dsb.config.FileConfig._
import com.hybris.core.dsb.model.InternalAppError
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._

/**
 * Functions to read configuration from user provided JSONs.
 */
trait FileConfig {

  /**
   * Reads configuration for backup operation.
   *
   * @param appConfig command line configuration
   * @return combined command line configuration and backup configuration
   */
  def readBackupConfig(appConfig: CliConfig): Xor[InternalAppError, (CliConfig, BackupConfig)] = {
    val file = File(appConfig.configFile)

    if (file.exists) {
      parse(file.contentAsString)
        .flatMap(json => json.as[BackupConfig])
        .leftMap(error => InternalAppError("Failed to decode configuration file" + error.getMessage))
        .map(backupConfig => (appConfig, backupConfig))
    } else {
      InternalAppError(s"Configuration file '${appConfig.configFile}' not found").left
    }
  }
}

object FileConfig {
  implicit val backupConfigDecoder: Decoder[BackupConfig] = deriveDecoder
  implicit val backupTenantConfigDecoder: Decoder[BackupTenantConfig] = deriveDecoder
}
