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
import com.hybris.core.dbr.exceptions.RestoreException
import com.hybris.core.dbr.file.FileOps._
import com.hybris.core.dbr.model.RestoreTypeData
import io.circe.Json

import scala.concurrent.ExecutionContext

trait RestoreStream {

  val Parallelism = 5

  def addDocuments(restoreDir: String): Flow[RestoreTypeConfig, RestoreTypeData, NotUsed] = {
    Flow[RestoreTypeConfig]
      .mapConcat { rtc =>
        readDocuments(s"$restoreDir/${rtc.file}") match {
          case Xor.Right(documents) =>
            documents.map(doc => RestoreTypeData(rtc.client, rtc.tenant, rtc.`type`, doc))

          case Xor.Left(error) =>
            throw new RestoreException(error.getMessage)
        }
      }
  }

  private def readDocuments(path: String): Xor[FileError, List[String]] = {
    readFileAs[List[Json]](path).map(jsons => jsons.map(_.noSpaces))
  }

  def insertDocuments(documentServiceClient: DocumentServiceClient)
                     (implicit executionContext: ExecutionContext): Flow[RestoreTypeData, String, NotUsed] = {
    Flow[RestoreTypeData]
      .mapAsync(Parallelism) { rtd =>
        documentServiceClient.insertRawDocument(rtd.client, rtd.tenant, rtd.`type`, rtd.document)
          .recover {
            case t: Throwable =>
              throw new RestoreException(t.getMessage)
          }
      }
  }
}
