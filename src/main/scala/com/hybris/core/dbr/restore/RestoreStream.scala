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

import akka.NotUsed
import akka.stream.scaladsl.Flow
import cats.data.Xor
import com.hybris.core.dbr.config.RestoreTypeConfig
import com.hybris.core.dbr.document.DocumentServiceClient
import com.hybris.core.dbr.model.RestoreTypeData

trait RestoreStream extends RestoreFileOps {

  val Parallelism = 5

  def addDocuments(restoreDir: String): Flow[RestoreTypeConfig, RestoreTypeData, NotUsed] = {
    Flow[RestoreTypeConfig]
      .mapConcat { rtc =>
        readDocuments(s"$restoreDir/${rtc.file}") match {
          case Xor.Right(documents) =>
            documents.map(doc => RestoreTypeData(rtc.client, rtc.tenant, rtc.`type`, doc))

          case Xor.Left(error) =>
            throw new RuntimeException(error.getMessage)
        }
      }
  }

  def insertDocuments(documentServiceClient: DocumentServiceClient): Flow[RestoreTypeData, String, NotUsed] = {
    Flow[RestoreTypeData]
      .mapAsync(Parallelism) { rtd =>
        documentServiceClient.insertRawDocument(rtd.client, rtd.tenant, rtd.`type`, rtd.document)
      }
  }
}
