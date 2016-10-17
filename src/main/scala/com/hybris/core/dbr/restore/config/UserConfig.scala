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

import cats.data.Xor
import com.hybris.core.dbr.restore.FileUtils
import com.hybris.core.dbr.restore.model.OwnerInfo
import io.circe._
import io.circe.parser._

trait UserConfig extends FileUtils {

  implicit val ownerInfoDecoder: Decoder[OwnerInfo] = Decoder.forProduct4("client", "tenant", "type", "file")(OwnerInfo.apply)

  def getRestoreConfig(configFile: String): List[OwnerInfo] = {
    val fileContents = readFile(configFile)

    val json = parse(fileContents).getOrElse(Json.Null)

    json.as[List[OwnerInfo]].value match {
      case Xor.Right(result) ⇒
        result
      case Xor.Left(e) ⇒
//        logger.error(s"Error parsing $configFile: ${e.message}")
        Nil
    }
  }
}
