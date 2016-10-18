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
package com.hybris.core.dbr.restore

import cats.data.Xor
import com.hybris.core.dbr.file.FileOps.{FileError, _}
import io.circe.Json

trait RestoreFileOps {

  def readDocuments(path: String): Xor[FileError, List[String]] = {
    readFileAs[List[Json]](path).map(jsons => jsons.map(_.noSpaces))
  }
}
