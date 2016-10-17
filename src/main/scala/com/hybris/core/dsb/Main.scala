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
package com.hybris.core.dsb

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import cats.data.Xor
import com.hybris.core.dsb.backup.BackupService
import com.hybris.core.dsb.config._
import com.hybris.core.dsb.document.DefaultDocumentServiceClient
import com.hybris.core.dsb.file.FileOps
import com.hybris.core.dsb.model.ClientTenant

import scala.util.{Failure, Success}

/**
 * Main class of backup tool.
 */
object Main extends App with Cli with FileConfig with AppConfig with FileOps {

  private def run(): Unit = {
    readCliConfig(args) match {
      case Some(cliConfig) => proceed(cliConfig)
      case None => // ignore and stop
    }
  }

  private def proceed(cliConfig: CliConfig): Unit = {
    val preparation = validateCliConfig(cliConfig)
      .flatMap(readBackupConfig)
      .flatMap {
        case (ac, bc) => prepareEmptyDir(ac.destinationDir).map(ready => (ac, bc))
      }

    preparation match {
      case Xor.Right((appConfig, backupConfig)) =>
        runBackup(appConfig, backupConfig)

      case Xor.Left(error) =>
        Console.out.println(error.message)
    }
  }

  private def runBackup(cliConfig: CliConfig, backupConfig: BackupConfig): Unit = {
    Console.out.println("Starting backup")

    implicit val system = ActorSystem("document-backup")
    implicit val materializer = ActorMaterializer()

    import system.dispatcher

    val credentials = prepareBasicHttpCredentials(documentHttpCredentials)

    val documentServiceClient = new DefaultDocumentServiceClient(documentUrl(cliConfig.env), credentials)

    val backupJob = new BackupService(documentServiceClient,
      cliConfig.destinationDir, summaryFileName)

    val cts = backupConfig.tenants.map(t => ClientTenant(cliConfig.client, t.tenant, t.types))

    backupJob.runBackup(cts) onComplete {
      case Success(_) =>
        Console.out.println("Backup done successfully")
        Http().shutdownAllConnectionPools() onComplete { _ =>
          materializer.shutdown()
          system.terminate()
        }

      case Failure(ex) =>
        Console.out.println("Backup failed with error: " + ex.getMessage)
        Http().shutdownAllConnectionPools() onComplete { _ =>
          materializer.shutdown()
          system.terminate()
        }
    }
  }

  private def prepareBasicHttpCredentials(credentials: String): Option[(String, String)] = {
    if (credentials.trim.isEmpty) None
    else {
      val idx = credentials.indexOf(":")
      if (idx == -1) None else Some((credentials.substring(0, idx), credentials.substring(idx + 1)))
    }
  }

  run()
}
