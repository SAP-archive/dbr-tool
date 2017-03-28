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
package com.hybris.core.dbr.index

import com.hybris.core.dbr.BaseTest
import com.hybris.core.dbr.model.IndexDefinition

class IndexUtilTest extends BaseTest {

  "IndexUtil" when {

    "preparing before index creation" should {

      "not change index definition when no weights in options" in {
        val keys =
          """
            |{
            |  "city" : 1,
            |  "state" : 1
            |}
          """.stripMargin

        val options =
          """
            |{
            |  "name" : "indexName"
            |}
          """.stripMargin

        // when
        val result = IndexUtil.prepareBeforeIndexCreation(IndexDefinition(toJson(keys), toJson(options)))

        // then
        result mustBe IndexDefinition(toJson(keys), toJson(options))
      }

      "not change index definition when weights for specified fields in options" in {
        val keys =
          """
            |{
            |  "city" : "text",
            |  "state" : "text"
            |}
          """.stripMargin

        val options =
          """
            |{
            |  "name" : "indexName",
            |  "weights" : {
            |    "city" : 10,
            |    "state" : 5
            |  }
            |}
          """.stripMargin

        // when
        val result = IndexUtil.prepareBeforeIndexCreation(IndexDefinition(toJson(keys), toJson(options)))

        // then
        result mustBe IndexDefinition(toJson(keys), toJson(options))
      }

      "remove weights from index definition's options when weights all fields in options" in {
        val keys =
          """
            |{
            |  "city" : "text",
            |  "state" : "text"
            |}
          """.stripMargin

        val optionsBefore =
          """
            |{
            |  "name" : "indexName",
            |  "weights" : {
            |    "$all" : 1
            |  }
            |}
          """.stripMargin

        val optionsAfter =
          """
            |{
            |  "name" : "indexName"
            |}
          """.stripMargin

        // when
        val result = IndexUtil.prepareBeforeIndexCreation(IndexDefinition(toJson(keys), toJson(optionsBefore)))

        // then
        result mustBe IndexDefinition(toJson(keys), toJson(optionsAfter))
      }
    }
  }

}
