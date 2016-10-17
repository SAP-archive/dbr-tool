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

import cats.data.Xor
import cats.implicits._
import com.hybris.core.dbr.model.InternalAppError


/**
 * Functions for command line interface.
 */
trait Cli extends AppConfig {

  private val allEnvironments = environments.mkString(", ")

  private val parser = new scopt.OptionParser[CliConfig](appName) {
    head(appName, appVersion, "- Document service backup/restore tool")

    opt[String]("env")
      .action((env, cfg) => cfg.copy(env = env))
      .text(s"environment, available options: $allEnvironments")
      .required()
      .validate { env =>
        if (environments.contains(env) || env == "local") success else failure(s"unknown environment '$env'")
      }

    opt[String]("client")
      .action((cl, cfg) => cfg.copy(client = cl))
      .text("hybris client")
      .required()

    opt[String]("config")
      .action((cf, cfg) => cfg.copy(configFile = cf))
      .text("path to configuration file for backup or restore")
      .required()

    note("")

    cmd("backup")
      .action((_, cfg) => cfg.copy(command = "backup"))
      .children {

        opt[String]("out")
          .action((dstDir, cfg) => cfg.copy(backupDestinationDir = dstDir))
          .text("destination folder")
          .required()

      }

    note("")

    cmd("restore")
      .action((_, cfg) => cfg.copy(command = "restore"))
      .children {

        opt[String]("dir")
          .action((srcDir, cfg) => cfg.copy(restoreSourceDir = srcDir))
          .text("directory with backup files")
          .required()

      }
  }

  def readCliConfig(args: Array[String]): Option[CliConfig] = {
    parser.parse(args, CliConfig())
  }

  def validateCliConfig(appConfig: CliConfig): Xor[InternalAppError, CliConfig] = {
    validateEnv(appConfig)
  }

  private def validateEnv(appConfig: CliConfig): Xor[InternalAppError, CliConfig] = {
    appConfig.env match {
      case "us-prod" | "us-stage" | "eu" => appConfig.right
      case env => InternalAppError(s"Wrong environment provided: '$env'. Acceptable values: us-prod, us-stage, eu").left
    }
  }
}
