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

import com.hybris.core.dbr.BaseTest

class CliTest extends BaseTest with Cli {

  "Cli" should {

    "read cli config for backup" in {

      val args = Array("backup", "--env", "us-prod", "--client", "hybris.space", "--config", "space.json", "--out", "/nasa/moon")

      readCliConfig(args).value mustBe CliConfig("backup", "us-prod", "hybris.space", "space.json", "/nasa/moon", "")
    }

    "read cli config for restore" in {

      val args = Array("restore", "--env", "us-prod", "--client", "hybris.space", "--config", "space.json", "--dir", "/nasa/moon")

      readCliConfig(args).value mustBe CliConfig("restore", "us-prod", "hybris.space", "space.json", "", "/nasa/moon")
    }

    "validate environment" in {

      val args = Array("restore", "--env", "moon", "--client", "hybris.space", "--config", "space.json", "--dir", "/nasa/moon")

      readCliConfig(args) mustBe None
    }
  }
}
