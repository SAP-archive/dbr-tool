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

import akka.event.slf4j.SLF4JLogging
import com.hybris.core.dbr.model.IndexDefinition
import io.circe.Json

object IndexUtil extends SLF4JLogging {

  def prepareBeforeIndexCreation(indexDefinition: IndexDefinition): IndexDefinition = {
    if (hasWeightsWithAll(indexDefinition.options)) {
      val newOptions = deleteWeights(indexDefinition.options)
      indexDefinition.copy(options = newOptions)
    } else {
      indexDefinition
    }
  }

  private def hasWeightsWithAll(json: Json): Boolean = {
    json.hcursor.downField("weights").downField("$all").succeeded
  }

  private def deleteWeights(json: Json): Json = {
    json.hcursor.downField("weights").delete.as[Json] match {
      case Right(result) =>
        result

      case Left(_) =>
        log.error("Failed to remove weights from index options")
        json
    }
  }
}
