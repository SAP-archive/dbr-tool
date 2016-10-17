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
import com.hybris.core.dbr.restore.BaseCoreTest
import com.hybris.core.dbr.restore.errors.ParseError
import com.hybris.core.dbr.restore.model.{OwnerDocuments, OwnerInfo}

class FileBackupRepositoryTest extends BaseCoreTest {

  "BackupFileRepository" should {

    "read backup file" in {
      // given
      val backupRepository = new FileBackupRepository("src/test/resources")
      val entry = OwnerInfo("client", "tenant", "type", "001.json")

      // when
      val readResult: Xor[ParseError, OwnerDocuments] = backupRepository.read(entry)

      // then
      readResult match {
        case Xor.Right(r) ⇒
          r.documents.size mustBe 2
        case _ ⇒
          fail()
      }
    }

    "read empty backup file" in {
      // given
      val backupRepository = new FileBackupRepository("src/test/resources")
      val entry = OwnerInfo("client", "tenant", "type", "empty.json")

      // when
      val readResult: Xor[ParseError, OwnerDocuments] = backupRepository.read(entry)

      // then
      readResult match {
        case Xor.Right(r) ⇒
          r.documents.size mustBe 0
        case _ ⇒
          fail()
      }
    }

    "read wrong backup file" in {
      // given
      val backupRepository = new FileBackupRepository("src/test/resources")
      val entry = OwnerInfo("client", "tenant", "type", "wrong.json")

      // when
      val readResult: Xor[ParseError, OwnerDocuments] = backupRepository.read(entry)

      // then
      readResult match {
        case Xor.Left(e) ⇒
          e mustBe a[ParseError]
        case _ ⇒
          fail()
      }
    }
  }
}
