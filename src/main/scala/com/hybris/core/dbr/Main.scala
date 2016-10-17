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
package com.hybris.core.dbr

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import cats.data.Xor
import com.hybris.core.dbr.backup.BackupService
import com.hybris.core.dbr.config._
import com.hybris.core.dbr.document.DefaultDocumentServiceClient
import com.hybris.core.dbr.file.FileOps
import com.hybris.core.dbr.model.ClientTenant
import com.hybris.core.dbr.restore.repository.FileBackupRepository
import com.hybris.core.dbr.restore.service.DocumentRestoreService

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Main class of backup tool.
 */
object Main extends App with Cli with FileConfig with AppConfig with FileOps {

  private def run(): Unit = {
    readCliConfig(args) match {
      case Some(cliConfig) if cliConfig.isBackup => proceedBackup(cliConfig)

      case Some(cliConfig) if cliConfig.isRestore => runRestore(cliConfig)

      case None => // ignore and stop
    }
  }

  private def proceedBackup(cliConfig: CliConfig): Unit = {
    val preparation = validateCliConfig(cliConfig)
      .flatMap(readBackupConfig)
      .flatMap {
        case (ac, bc) => prepareEmptyDir(ac.backupDestinationDir).map(ready => (ac, bc))
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

    implicit val system = ActorSystem("dbr")
    implicit val materializer = ActorMaterializer()

    import system.dispatcher

    val credentials = prepareBasicHttpCredentials(documentHttpCredentials)

    val documentServiceClient = new DefaultDocumentServiceClient(documentUrl(cliConfig.env), credentials)

    val backupJob = new BackupService(documentServiceClient,
      cliConfig.backupDestinationDir, summaryFileName)

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

  private def runRestore(cliConfig: CliConfig): Unit = {
    Console.out.println("Starting restore")

    implicit val system = ActorSystem("dbr")
    implicit val materializer = ActorMaterializer()

    import system.dispatcher

    val credentials = prepareBasicHttpCredentials(documentHttpCredentials)

    val documentServiceClient = new DefaultDocumentServiceClient(documentUrl(cliConfig.env), credentials)

    val backupFileRepository = new FileBackupRepository(cliConfig.restoreSourceDir)

    val restoreService = new DocumentRestoreService(documentServiceClient, backupFileRepository)

    Future.sequence(getRestoreConfig(cliConfig.configFile).map(restoreService.restore)).onComplete {
      case Success(_) =>
        Console.out.println("Restore done successfully")
        Http().shutdownAllConnectionPools() onComplete { _ =>
          materializer.shutdown()
          system.terminate()
        }

      case Failure(ex) =>
        Console.out.println("Restore failed with error: " + ex.getMessage)
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
