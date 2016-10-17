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
package com.hybris.core.dbr.restore.repository

import cats.data.Xor
import com.hybris.core.dbr.restore.FileUtils
import com.hybris.core.dbr.restore.errors.ParseError
import com.hybris.core.dbr.restore.model.{OwnerDocuments, OwnerInfo}
import io.circe.Json
import io.circe.parser._

/**
 * Repository for managing backups.
 */
trait BackupRepository {
  def read(ownerInfo: OwnerInfo): Xor[ParseError, OwnerDocuments]
}

class FileBackupRepository(backupDirectory: String) extends FileUtils with BackupRepository {

  def read(ownerInfo: OwnerInfo): Xor[ParseError, OwnerDocuments] = {
    val document = parse(readFile(s"$backupDirectory/${ownerInfo.fileName}")).getOrElse(Json.Null)

    val data = for {
      documents ← document.hcursor.as[List[Json]]
    } yield {
        OwnerDocuments(ownerInfo, documents.map(_.toString()))
      }

    data.leftMap(e ⇒ ParseError(s"Error parsing json: ${e.message}"))
  }
}
