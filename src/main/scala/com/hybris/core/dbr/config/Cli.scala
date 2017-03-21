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

/**
 * Functions for command line interface.
 */
trait Cli extends AppConfig {

  private val allEnvironments = environments.mkString(", ")

  private val parser = new scopt.OptionParser[CliConfig](appName) {

    override def showUsageOnError = true

    head(appName, appVersion, "- Document Service Backup/Restore Tool")

    opt[String]("env")
      .action((env, cfg) => cfg.copy(env = env))
      .text(s"Environment, available options: [$allEnvironments].")
      .required()
      .validate { env =>
        if (environments.contains(env) || env == "local") success else failure(s"Unknown environment '$env'.")
      }

    help("help").text("Prints this usage text.")

    note("")

    cmd("backup")
      .action((_, cfg) => cfg.copy(command = "backup"))
      .children(
        opt[String]("config")
          .action((cf, cfg) => cfg.copy(configFile = cf))
          .text("Path to backup configuration file.")
          .required(),
        opt[String]("out")
          .action((dstDir, cfg) => cfg.copy(backupDestinationDir = dstDir))
          .text("Destination directory.")
          .required(),
        opt[String]("client")
          .action((cl, cfg) => cfg.copy(client = cl))
          .text("YaaS client.")
          .required(),
        opt[Unit]("skipIndexes")
          .action((_, cfg) => cfg.copy(skipIndexes = true))
          .text("Skip index backup.")
          .optional()
      )

    note("")

    cmd("restore")
      .action((_, cfg) => cfg.copy(command = "restore"))
      .children(
        opt[String]("dir")
          .action((srcDir, cfg) => cfg.copy(restoreSourceDir = srcDir))
          .text("Directory with backup files.")
          .required()
      )
  }

  def readCliConfig(args: Array[String]): Option[CliConfig] = {
    parser.parse(args, CliConfig())
  }

}
