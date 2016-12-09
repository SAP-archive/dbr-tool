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
import com.hybris.core.dbr.model.RestoreTypeData

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

trait RestoreStream extends SLF4JLogging with AppConfig {

  val Parallelism = 1

  def addDocuments(restoreDir: String): Flow[RestoreTypeConfig, RestoreTypeData, NotUsed] = {
    Flow[RestoreTypeConfig]
      .flatMapConcat(config ⇒ {
          val fileSource: Source[Seq[ByteString], Future[IOResult]] =
            getFileSource(s"$restoreDir/${config.file}", jsonByJson)
              .grouped(documentsUploadChunk)

        fileSource.map{ documents ⇒
          RestoreTypeData(config.client, config.tenant, config.`type`, Source(documents))
        }
      })
  }

  private def getFileSource(fileName: String,
                            readStrategy: Source[ByteString, Future[IOResult]] ⇒ Source[ByteString, Future[IOResult]]): Source[ByteString, Future[IOResult]] = {
    val file = Paths.get(fileName)
    if (Files.exists(file)) {
      readStrategy(FileIO.fromPath(file))
    }
    else {
      throw RestoreException(s"File '$file' not found.")
    }
  }

  private def jsonByJson(fileSource: Source[ByteString, Future[IOResult]]) = {
    fileSource
      .via(JsonFraming.objectScanner(readFileChunkSize))
      .intersperse(ByteString("["), ByteString(","), ByteString("]"))
      .filterNot(_.startsWith(","))
  }

  def insertDocuments(documentBackupClient: DocumentBackupClient)
                     (implicit executionContext: ExecutionContext): Flow[RestoreTypeData, InsertResult, NotUsed] = {
    Flow[RestoreTypeData]
      .mapAsync(Parallelism) { rtd =>
        documentBackupClient.insertDocuments(rtd.client, rtd.tenant, rtd.`type`, rtd.documents)
          .recover {
            case t: Throwable => throw RestoreException(t.getMessage)
          }.map { ir ⇒
          log.info(s"Type '${rtd.`type`}' in tenant '${rtd.tenant}' restored. " +
            s"Processed ${ir.totalDocuments} documents (${ir.inserted} inserted, ${ir.replaced} replaced).")
          ir
        }
      }
  }
}
