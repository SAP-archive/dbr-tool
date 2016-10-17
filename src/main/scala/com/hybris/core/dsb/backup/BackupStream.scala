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
package com.hybris.core.dsb.backup

import java.nio.file.Paths
import java.util.UUID

import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Flow}
import akka.{Done, NotUsed}
import better.files.File
import com.hybris.core.dsb.document.DocumentServiceClient
import com.hybris.core.dsb.model.{ClientTenant, ClientTenantType, TypeBackupData, TypeBackupResult}
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax.EncoderOps

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Components of backup stream.
 */
trait BackupStream {

  val Parallelism = 5

  val DataParallelism = 1

  implicit val ble: Encoder[TypeBackupResult] = deriveEncoder

  def addTypes(documentServiceClient: DocumentServiceClient)
              (implicit executionContext: ExecutionContext): Flow[ClientTenant, ClientTenant, NotUsed] = {
    Flow[ClientTenant]
      .mapAsync(Parallelism) { ct =>
        ct.types match {
          case Some(types) => Future.successful(ct)
          case None => documentServiceClient.getTypes(ct.client, ct.tenant).map(types => ct.copy(types = Some(types)))
        }
      }
  }

  val flattenTypes: Flow[ClientTenant, ClientTenantType, NotUsed] = {
    Flow[ClientTenant]
      .mapConcat {
        case ClientTenant(client, tenant, Some(types)) if types.nonEmpty =>
          types.map(`type` => ClientTenantType(client, tenant, `type`))
        case _ =>
          List()
      }
  }

  def addDocuments(documentServiceClient: DocumentServiceClient)
                  (implicit executionContext: ExecutionContext): Flow[ClientTenantType, TypeBackupData, NotUsed] = {
    Flow[ClientTenantType]
      .mapAsync(DataParallelism) { ctt =>
        documentServiceClient.getDocuments(ctt.client, ctt.tenant, ctt.`type`)
          .map { data =>
            TypeBackupData(ctt.client, ctt.tenant, ctt.`type`, data)
          }
      }
  }

  def writeToFiles(destinationDir: String)
                  (implicit executionContext: ExecutionContext,
                   materializer: Materializer): Flow[TypeBackupData, TypeBackupResult, NotUsed] = {
    Flow[TypeBackupData]
      .mapAsync(DataParallelism) { tbd =>
        val fileName = UUID.randomUUID().toString + ".json"
        val path = Paths.get(s"$destinationDir/$fileName")

        tbd.data.runWith(FileIO.toPath(path))
          .flatMap { ioResult =>
            ioResult.status match {
              case Success(Done) =>
                Future.successful(TypeBackupResult(tbd.client, tbd.tenant, tbd.`type`, fileName))
              case Failure(ex) =>
                Future.failed(ex)
            }
          }
      }
  }

  def writeSummary(destinationDir: String, fileName: String): Flow[TypeBackupResult, Done, NotUsed] = {
    Flow[TypeBackupResult]
      .fold(List[TypeBackupResult]())((acc, tbr) => acc :+ tbr)
      .map { summary =>
        val file = File(s"$destinationDir/$fileName")
        file.overwrite(summary.asJson.spaces4)
        Done
      }
  }

}
