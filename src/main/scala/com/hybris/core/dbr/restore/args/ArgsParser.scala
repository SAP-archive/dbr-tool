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
package com.hybris.core.dbr.restore.args

import com.hybris.core.dbr.restore.config.MainConfig

trait ArgsParser extends ArgsNames with MainConfig {

  val name = config.getString("info.name")

  val parser = new scopt.OptionParser[Args](name) {
    head(name)

    val Envs = List("us-stage", "us-prod", "eu", "local")

    opt[String](EnvironmentArg)
      .action((x, c) => c.copy(env = x))
      .required()
      .text(s"Document service environment. (${Envs.filterNot(_ == "local").mkString("|")})")
      .validate { e ⇒
        if (Envs.contains(e)) {
          success
        }
        else {
          failure(s"env argument must be in (${Envs.filterNot(_ == "local").mkString("|")}).")
        }
      }

    opt[String](UsernameArg)
      .action((x, c) => c.copy(username = x))
      .required()
      .text("Document service username.")

    opt[String](PasswordArg)
      .action((x, c) => c.copy(password = x))
      .required()
      .text("Document service password.")

    opt[String](ConfigArg)
      .action((x, c) => c.copy(config = x))
      .required()
      .text("JSON configuration file.")

    opt[String](DirectoryArg)
      .action((x, c) => c.copy(directory = x))
      .required()
      .text("Directory with backup files.")
  }
}
