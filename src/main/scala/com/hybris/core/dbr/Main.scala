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
import akka.stream.ActorMaterializer
import cats.implicits._
import com.hybris.core.dbr.backup.BackupService
import com.hybris.core.dbr.config._
import com.hybris.core.dbr.document.{DefaultDocumentBackupClient, DefaultDocumentServiceClient}
import com.hybris.core.dbr.file.FileOps._
import com.hybris.core.dbr.model.{ClientTenant, InternalAppError}
import com.hybris.core.dbr.oauth.OAuthClient
import com.hybris.core.dbr.restore.RestoreService
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
 * Main class of backup tool.
 */
object Main extends App with Cli with FileConfig with AppConfig with LazyLogging {

  private def run(): Unit = {
    readCliConfig(args) match {
      case Some(cliConfig) if cliConfig.isBackup => runBackup(cliConfig)

      case Some(cliConfig) if cliConfig.isRestore => runRestore(cliConfig)

      case None => // ignore and stop
    }
  }

  private def runBackup(cliConfig: CliConfig) = {
    prepareBackup(cliConfig) match {
      case Right(backupConfig) =>
        doBackup(cliConfig, backupConfig)

      case Left(error) =>
        logger.error(error.message)
    }
  }

  private def prepareBackup(cliConfig: CliConfig): Either[InternalAppError, BackupConfig] = {
    readBackupConfig(cliConfig.configFile)
      .flatMap { backupConfig =>
        prepareEmptyDir(cliConfig.backupDestinationDir)
          .map(ready => backupConfig)
          .leftMap(fileError => InternalAppError(fileError.getMessage))
      }
  }

  private def doBackup(cliConfig: CliConfig, backupConfig: BackupConfig): Unit = {
    logger.info(s"Starting backup of client: ${cliConfig.client}")

    implicit val system = ActorSystem("dbr")
    implicit val materializer = ActorMaterializer()

    import system.dispatcher

    val oauthClient = new OAuthClient(oauthUrl(cliConfig.env), clientId, clientSecret, scopes)

    val result = getOAuthToken(cliConfig.env, oauthClient)
      .flatMap { token =>
        logger.info("OAuth token successfully acquired.")

        val documentBackupClient = new DefaultDocumentBackupClient(documentBackupUrl(cliConfig.env), token)
        val documentServiceClient = new DefaultDocumentServiceClient(documentServiceUrl(cliConfig.env), token)

        val backupJob = new BackupService(documentBackupClient, documentServiceClient, cliConfig.backupDestinationDir, summaryFileName)

        val cts = backupConfig.tenants.map(t => ClientTenant(cliConfig.client, t.tenant, t.types.getOrElse(List())))

        backupJob.runBackup(cts)
      }

    result.onComplete {
      case Success(_) =>
        logger.info("Backup done successfully")
        system.terminate()

      case Failure(ex) =>
        logger.error("Backup failed with error: " + ex.getMessage)
        system.terminate()
    }
  }

  private def runRestore(cliConfig: CliConfig) = {
    prepareRestore(cliConfig) match {
      case Right(restoreConfig) =>
        doRestore(cliConfig, restoreConfig)

      case Left(error) =>
        logger.error(error.message)
    }
  }

  private def prepareRestore(cliConfig: CliConfig): Either[InternalAppError, RestoreConfig] = {
    readRestoreConfig(s"${cliConfig.restoreSourceDir}/$summaryFileName")
  }

  private def doRestore(cliConfig: CliConfig, restoreConfig: RestoreConfig): Unit = {
    logger.info(s"Starting restore of client: ${cliConfig.client}")

    implicit val system = ActorSystem("dbr")
    implicit val materializer = ActorMaterializer()

    import system.dispatcher

    val oauthClient = new OAuthClient(oauthUrl(cliConfig.env), clientId, clientSecret, scopes)

    val result = getOAuthToken(cliConfig.env, oauthClient)
      .flatMap { token =>
        val documentBackupClient = new DefaultDocumentBackupClient(documentBackupUrl(cliConfig.env), token)

        val restoreService = new RestoreService(documentBackupClient, cliConfig.restoreSourceDir)

        restoreService.restore(restoreConfig.types)
      }

    result onComplete {
      case Success(_) =>
        logger.info("Restore done successfully")
        system.terminate()

      case Failure(ex) =>
        logger.error("Restore failed with error: " + ex.getMessage)
        ex.printStackTrace()
        system.terminate()
    }
  }

  private def getOAuthToken(env: String, oauthClient: OAuthClient)(implicit ec: ExecutionContext): Future[Option[String]] = {
    if (env == "local") Future.successful(None) else oauthClient.getToken.map(Some(_))
  }

  run()
}
