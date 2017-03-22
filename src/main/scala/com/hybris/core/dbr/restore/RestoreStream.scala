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
import akka.stream.scaladsl.{FileIO, Flow, JsonFraming, Source}
import akka.stream.{IOResult, Materializer}
import akka.util.ByteString
import com.hybris.core.dbr.config.{AppConfig, RestoreTypeDefinition}
import com.hybris.core.dbr.document.{DocumentBackupClient, DocumentServiceClient, InsertResult}
import com.hybris.core.dbr.exceptions.RestoreException
import com.hybris.core.dbr.model.IndexDefinition

import scala.concurrent.{ExecutionContext, Future}

trait RestoreStream extends SLF4JLogging with AppConfig {

  val Parallelism = 1

  def createIndexes(documentServiceClient: DocumentServiceClient)
                   (implicit executionContext: ExecutionContext,
                    materializer: Materializer): Flow[RestoreTypeDefinition, RestoreTypeDefinition, NotUsed] = {
    Flow[RestoreTypeDefinition]
      .mapAsync(Parallelism) { rtd ⇒
        rtd.indexes match {
          case Some(indexes) ⇒
            createIndexesInternal(indexes, documentServiceClient, rtd)

          case None ⇒
            Future.successful(rtd)
        }
      }
  }

  def insertType(restoreDir: String, documentBackupClient: DocumentBackupClient)
                (implicit executionContext: ExecutionContext): Flow[RestoreTypeDefinition, InsertResult, NotUsed] = {
    Flow[RestoreTypeDefinition]
      .flatMapConcat(config ⇒ {
        log.info(s"Restoring tenant '${config.tenant}' type '${config.`type`}'")
        configToFileChunksSource(restoreDir, config)
          .via(insertDocuments(documentBackupClient, config))
          .via(aggregateAndLogResults(config))
      })
  }

  private[restore] def configToFileChunksSource(restoreDir: String, rtc: RestoreTypeDefinition): Source[Source[ByteString, _], _] = {
    getFileSource(s"$restoreDir/${rtc.file}")
      .via(JsonFraming.objectScanner(readFileChunkSize))
      .grouped(documentsUploadChunk)
      .map(Source(_))
  }

  private[restore] def insertDocuments(documentBackupClient: DocumentBackupClient, rtc: RestoreTypeDefinition)
                                      (implicit executionContext: ExecutionContext): Flow[Source[ByteString, _], InsertResult, NotUsed] = {
    Flow[Source[ByteString, _]]
      .mapAsync(Parallelism) { rtd =>
        documentBackupClient.insertDocuments(rtc.client, rtc.tenant, rtc.`type`, rtd)
          .recover {
            case t: Throwable => throw RestoreException(t.getMessage)
          }.map { ir ⇒
          if (ir.totalDocuments == documentsUploadChunk) {
            val msg = s"\t - Restoring tenant '${rtc.tenant}' type '${rtc.`type`}':" +
              s" ${ir.totalDocuments} documents, " +
              s"(${ir.inserted} inserted, " +
              s"${ir.replaced} replaced)."

            log.info(msg)
          }
          ir
        }
      }
  }

  private def createIndexesInternal(indexes: List[IndexDefinition], documentServiceClient: DocumentServiceClient, rtd: RestoreTypeDefinition)
                                   (implicit materializer: Materializer,
                                    executionContext: ExecutionContext) =
    Source(indexes)
      .mapAsync(Parallelism)(id ⇒ documentServiceClient.createIndex(rtd.client, rtd.tenant, rtd.`type`, id))
      .runFold(List[String]())((acc, id) ⇒ id :: acc)
      .map(res ⇒ {
        log.info(s"${res.size} index(es) restored: ${res.mkString(", ")}")
        rtd
      })

  private[restore] def aggregateAndLogResults(rtc: RestoreTypeDefinition): Flow[InsertResult, InsertResult, NotUsed] =
    Flow[InsertResult]
      .fold(InsertResult(0, 0, 0))((acc, t) ⇒ acc.copy(
        totalDocuments = acc.totalDocuments + t.totalDocuments,
        inserted = acc.inserted + t.inserted,
        replaced = acc.replaced + t.replaced)
      )
      .map(ir ⇒ {
        val msg = s"Restored tenant '${rtc.tenant}' type '${rtc.`type`}'. " +
          s"Total ${ir.totalDocuments} documents, (${ir.inserted} inserted, ${ir.replaced} replaced)."
        log.info(msg)
        ir
      })

  private def getFileSource(fileName: String): Source[ByteString, Future[IOResult]] = {
    val file = Paths.get(fileName)
    if (Files.exists(file)) {
      FileIO.fromPath(file)
    }
    else {
      Source.failed(RestoreException(s"File '$file' not found.")).asInstanceOf[Source[ByteString, Future[IOResult]]]
    }
  }
}
