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

import org.scalatest.{MustMatchers, WordSpec}

class ArgsParserTest extends WordSpec with MustMatchers {

  object ArgsParser extends ArgsParser

  "ArgsParser" should {

    "parse proper input" in {
      import ArgsParser.parser._

      parse(getProperArgs("us-stage"), Args()) mustBe Some(Args("us-stage", "user", "password", "config.json", "backup"))
      parse(getProperArgs("us-prod"), Args()) mustBe Some(Args("us-prod", "user", "password", "config.json", "backup"))
      parse(getProperArgs("eu"), Args()) mustBe Some(Args("eu", "user", "password", "config.json", "backup"))
    }

    "parse wrong input" in {
      import ArgsParser.parser._

      parse(getProperArgs("WRONG!!!"), Args()) mustBe None

      parse(Seq("--username", "user",
        "--password", "password",
        "--config", "config.json",
        "--dir", "backup"), Args()) mustBe None

      parse(Seq("--env", "us-stage",
        "--config", "config.json",
        "--dir", "backup"), Args()) mustBe None

      parse(Seq("--env", "us-prod",
        "--username", "user",
        "--password", "password",
        "--dir", "backup"), Args()) mustBe None

      parse(Seq("--env", "us-prod",
        "--username", "user",
        "--password", "password"), Args()) mustBe None
    }
  }

  private def getProperArgs(env: String) =
    Seq("--env", env,
      "--username", "user",
      "--password", "password",
      "--config", "config.json",
      "--dir", "backup")
}
