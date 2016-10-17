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
package com.hybris.core.dbr.restore.config

import com.hybris.core.dbr.restore.BaseCoreTest
import com.hybris.core.dbr.restore.model.OwnerInfo

class UserConfigTest extends BaseCoreTest {

  object UserConfig extends UserConfig

  "UserConfig" should {
    "get restore config" in {
      // when
      val ownerInfoList = UserConfig.getRestoreConfig("src/test/resources/config.json")

      // then
      ownerInfoList mustBe List(OwnerInfo("framefrog.mycomicsshop", "framefrog", "cats", "01.json"),
        OwnerInfo("framefrog.mycomicsshop", "framefrog", "comic", "02.json"))

    }

    "get empty list if config is corrupted" in {
      // when
      val ownerInfoList = UserConfig.getRestoreConfig("src/test/resources/corruptedConfig.json")

      // then
      ownerInfoList mustBe Nil
    }
  }

}
