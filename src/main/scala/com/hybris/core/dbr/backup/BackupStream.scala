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

import java.nio.file.Paths
import java.util.UUID

import akka.event.slf4j.SLF4JLogging
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Flow}
import akka.{Done, NotUsed}
import better.files.File
import com.hybris.core.dbr.document.DocumentServiceClient
import com.hybris.core.dbr.model._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax.EncoderOps

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Components of backup stream.
 */
trait BackupStream extends SLF4JLogging {

  val Parallelism = 5

  val DataParallelism = 1

  implicit val typeBackupResultEncoder: Encoder[BackupTypeResult] = deriveEncoder

  def addTypes(documentServiceClient: DocumentServiceClient)
              (implicit executionContext: ExecutionContext): Flow[ClientTenant, ClientTenant, NotUsed] = {
    Flow[ClientTenant]
      .mapAsync(Parallelism) { ct =>
        ct.types match {
          case Some(types) => Future.successful(ct)
          case None =>
            val result = documentServiceClient
              .getTypes(ct.client, ct.tenant)
              .map(types => ct.copy(types = Some(types)))

            result.onSuccess {
              case elem =>
                val typesStr = elem.types.map(_.mkString(", ")).getOrElse("none")
                log.info(s"Types [$typesStr] for tenant '${elem.tenant}' received.")
            }

            result
        }
      }
  }

  val flattenTypes: Flow[ClientTenant, BackupType, NotUsed] = {
    Flow[ClientTenant]
      .mapConcat {
        case ClientTenant(client, tenant, Some(types)) if types.nonEmpty =>
          types.map(`type` => BackupType(client, tenant, `type`))
        case _ =>
          List()
      }
  }

  def addDocuments(documentServiceClient: DocumentServiceClient)
                  (implicit executionContext: ExecutionContext): Flow[BackupType, BackupTypeData, NotUsed] = {
    Flow[BackupType]
      .mapAsync(DataParallelism) { bt =>
        documentServiceClient.getDocuments(bt.client, bt.tenant, bt.`type`)
          .map { data =>
            BackupTypeData(bt.client, bt.tenant, bt.`type`, data)
          }
      }
  }

  def writeToFiles(destinationDir: String)
                  (implicit executionContext: ExecutionContext,
                   materializer: Materializer): Flow[BackupTypeData, BackupTypeResult, NotUsed] = {
    Flow[BackupTypeData]
      .mapAsync(DataParallelism) { btd =>
        val fileName = UUID.randomUUID().toString + ".json"
        val path = Paths.get(s"$destinationDir/$fileName")

        btd.data.runWith(FileIO.toPath(path))
          .flatMap { ioResult =>
            ioResult.status match {
              case Success(Done) =>
                log.info(s"Documents for tenant '${btd.tenant}' " +
                  s"and type '${btd.`type`}' written to file.")
                Future.successful(BackupTypeResult(btd.client, btd.tenant, btd.`type`, fileName))
              case Failure(ex) =>
                Future.failed(ex)
            }
          }
      }
  }

  def writeSummary(destinationDir: String, fileName: String): Flow[BackupTypeResult, Done, NotUsed] = {
    Flow[BackupTypeResult]
      .fold(List[BackupTypeResult]())((acc, btr) => acc :+ btr)
      .map { summary =>
        val file = File(s"$destinationDir/$fileName")
        file.overwrite(summary.asJson.spaces4)
        Done
      }
  }

}
