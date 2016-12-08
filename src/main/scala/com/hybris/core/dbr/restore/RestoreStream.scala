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

import java.nio.file.{Files, Paths}

import akka.NotUsed
import akka.event.slf4j.SLF4JLogging
import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Flow, Source}
import akka.util.ByteString
import com.hybris.core.dbr.config.RestoreTypeConfig
import com.hybris.core.dbr.document.{DocumentBackupClient, InsertResult}
import com.hybris.core.dbr.exceptions.RestoreException
import com.hybris.core.dbr.model.RestoreTypeData

import scala.concurrent.{ExecutionContext, Future}

trait RestoreStream extends SLF4JLogging {

  val Parallelism = 5

  def addDocuments(restoreDir: String): Flow[RestoreTypeConfig, RestoreTypeData, NotUsed] = {
    Flow[RestoreTypeConfig]
      .map(config ⇒ RestoreTypeData(config.client, config.tenant, config.`type`, getFileSource(s"$restoreDir/${config.file}")))
  }

  private def getFileSource(fileName: String): Source[ByteString, Future[IOResult]] = {
    val file = Paths.get(fileName)
    if (Files.exists(file)) {
      FileIO.fromPath(file)
    }
    else {
      throw RestoreException(s"File '$file' not found.")
    }
  }

  def insertDocuments(documentBackupClient: DocumentBackupClient)
                     (implicit executionContext: ExecutionContext): Flow[RestoreTypeData, InsertResult, NotUsed] = {
    Flow[RestoreTypeData]
      .mapAsync(Parallelism) { rtd =>
        documentBackupClient.insertDocuments(rtd.client, rtd.tenant, rtd.`type`, rtd.documents)
          .recover {
            case t: Throwable => throw RestoreException(t.getMessage)
          }.map { ir ⇒
          log.info(s"Client [${rtd.client}] Tenant [${rtd.tenant}] Type [${rtd.`type`}] - Processed ${ir.totalDocuments} documents: ${ir.inserted} inserted, ${ir.replaced} replaced.")
          ir
        }
      }
  }
}
