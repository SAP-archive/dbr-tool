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
package com.hybris.core.dbr.config

import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._

/**
  * Application's main configuration, read from application.conf.
  */
trait AppConfig {

  private lazy val config = ConfigFactory.load()

  lazy val appName: String = BuildInfo.name
  lazy val appVersion: String = BuildInfo.version

  lazy val environments: List[AnyRef] = config.getList("environments.keys").unwrapped().asScala.toList

  lazy val summaryFileName: String = config.getString("backup.summary-file-name")

  def documentServiceUrl(env: String): String = config.getString(s"environments.$env.document-url")

  def documentBackupUrl(env: String): String = config.getString(s"environments.$env.document-backup-url")

  def oauthUrl(env: String): String = config.getString(s"environments.$env.oauth-url")

  val clientId: String = config.getString("api.client.id")

  val clientSecret: String = config.getString("api.client.secret")

  val scopes: List[String] = config.getStringList("api.scopes").asScala.toList

  val readFileChunkSize: Int = config.getInt("restore.read-file-chunk-size")

  val documentsUploadChunk: Int = config.getInt("restore.no-documents-per-request")

}
