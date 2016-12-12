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
import akka.stream.scaladsl.{FileIO, Flow, JsonFraming, Source}
import akka.util.ByteString
import com.hybris.core.dbr.config.{AppConfig, RestoreTypeConfig}
import com.hybris.core.dbr.document.{DocumentBackupClient, InsertResult}
import com.hybris.core.dbr.exceptions.RestoreException

import scala.concurrent.{ExecutionContext, Future}

trait RestoreStream extends SLF4JLogging with AppConfig {

  val Parallelism = 1

  def insertType(restoreDir: String, documentBackupClient: DocumentBackupClient)
                (implicit executionContext: ExecutionContext): Flow[RestoreTypeConfig, InsertResult, NotUsed] = {
    Flow[RestoreTypeConfig]
      .flatMapConcat(config ⇒ {
        log.info(s"Restoring '${config.tenant}/${config.client}/${config.`type`}':")
        getFileSource(s"$restoreDir/${config.file}")
          .via(JsonFraming.objectScanner(readFileChunkSize))
          .grouped(documentsUploadChunk)
          .mapAsync(Parallelism) { rtd =>
            documentBackupClient.insertDocuments(config.client, config.tenant, config.`type`, Source(rtd))
              .recover {
                case t: Throwable => throw RestoreException(t.getMessage)
              }.map { ir ⇒
              log.info(s"\t - Restoring ${ir.totalDocuments} documents (${ir.inserted} inserted, ${ir.replaced} replaced).")
              ir
            }
          }
          .fold(InsertResult(0, 0, 0))((acc, t) ⇒ acc.copy(
            totalDocuments = acc.totalDocuments + t.totalDocuments,
            inserted = acc.inserted + t.inserted,
            replaced = acc.replaced + t.replaced)
          )
          .map(ir ⇒ {
            if (ir.totalDocuments > 1000) {
              log.info(s"Restoring '${config.tenant}/${config.client}/${config.`type`}' done!")
              log.info(s"Restored ${ir.totalDocuments} documents (${ir.inserted} inserted, ${ir.replaced} replaced).\n")
            }
            ir
          })
      })
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
}
