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

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}
import com.hybris.core.dbr.config.RestoreTypeDefinition
import com.hybris.core.dbr.document.{DocumentBackupClient, DocumentServiceClient}

import scala.concurrent.{ExecutionContext, Future}

class RestoreService(documentBackupClient: DocumentBackupClient,
                     documentServiceClient: DocumentServiceClient,
                     restoreDir: String,
                     skipIndexes: Boolean)
                    (implicit executionContext: ExecutionContext, materializer: Materializer)
  extends RestoreStream {

  def restore(types: List[RestoreTypeDefinition]): Future[Done] =
    createGraph(types).run()

  private def createGraph(types: List[RestoreTypeDefinition]): RunnableGraph[Future[Done]] = {
    val typesSource = Source(types)
    val typesSourceWithIndexes = if (skipIndexes) typesSource else typesSource.via(createIndexes(documentServiceClient))

    typesSourceWithIndexes
      .via(insertType(restoreDir, documentBackupClient))
      .toMat(Sink.ignore)(Keep.right)
  }
}
