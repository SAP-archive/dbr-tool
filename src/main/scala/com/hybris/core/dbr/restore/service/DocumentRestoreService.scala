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
package com.hybris.core.dbr.restore.service

import akka.actor.ActorSystem
import cats.data.Xor
import com.hybris.core.dbr.document.DocumentServiceClient
import com.hybris.core.dbr.restore.exceptions.ParseException
import com.hybris.core.dbr.restore.model.{OwnerDocuments, OwnerInfo}
import com.hybris.core.dbr.restore.repository.BackupRepository

import scala.concurrent.{ExecutionContext, Future}

/**
 * Service for restoring data.
 */
trait RestoreService {
  def restore(ownerInfo: OwnerInfo)(implicit sys: ActorSystem, ec: ExecutionContext): Future[List[String]]

}

class DocumentRestoreService(documentServiceClient: DocumentServiceClient, backupFileRepository: BackupRepository) {

  def restore(ownerInfo: OwnerInfo)(implicit sys: ActorSystem, ec: ExecutionContext): Future[List[String]] =
    backupFileRepository.read(ownerInfo) match {
      case Xor.Right(od) ⇒
        insertRawDocuments(od)
      case Xor.Left(e) ⇒
        Future.failed(new ParseException(s"Error parsing file: ${ownerInfo.fileName}"))
    }

  private def insertRawDocuments(ownerDocuments: OwnerDocuments)
                                (implicit sys: ActorSystem, ec: ExecutionContext): Future[List[String]] = {
    val tenant = ownerDocuments.owner.tenant
    val client = ownerDocuments.owner.client
    val `type` = ownerDocuments.owner.`type`

    Future.sequence(ownerDocuments.documents.map(documentServiceClient.insertRawDocument(tenant, client, `type`, _)))
  }
}
