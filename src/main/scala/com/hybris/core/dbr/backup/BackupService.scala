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
package com.hybris.core.dbr.backup

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}
import com.hybris.core.dbr.document.{DocumentBackupClient, DocumentServiceClient}
import com.hybris.core.dbr.model.ClientTenant

import scala.concurrent.{ExecutionContext, Future}

class BackupService(documentBackupClient: DocumentBackupClient,
                    documentServiceClient: DocumentServiceClient,
                    destinationDir: String,
                    summaryFileName: String,
                    backupIndexes: Boolean)
                   (implicit executionContext: ExecutionContext, materializer: Materializer) extends BackupStream {

  def runBackup(cts: List[ClientTenant]): Future[Done] = createGraph(cts).run()

  private def createGraph(cts: List[ClientTenant]): RunnableGraph[Future[Done]] = {
    val startStream = Source(cts)
      .via(addTypes(documentServiceClient))
      .via(flattenTypes)
      .via(addDocuments(documentBackupClient))
      .via(writeToFiles(destinationDir))

    val indexStream = if (backupIndexes) {
      startStream.via(addIndexes(documentServiceClient))
    } else {
      startStream
    }

    indexStream
      .via(writeSummary(destinationDir, summaryFileName))
      .toMat(Sink.head)(Keep.right)
  }

}
