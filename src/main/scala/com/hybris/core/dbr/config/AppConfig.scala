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

  lazy val appName = BuildInfo.name
  lazy val appVersion = BuildInfo.version

  lazy val environments = config.getList("environments.keys").unwrapped().asScala.toList

  lazy val summaryFileName = config.getString("summary-file-name")

  def documentServiceUrl(env: String): String = config.getString(s"environments.$env.document-url")
  def documentBackupUrl(env: String): String = config.getString(s"environments.$env.document-backup-url")

  def oauthUrl(env: String): String = config.getString(s"environments.$env.oauth-url")

  val clientId = config.getString("api.client.id")

  val clientSecret = config.getString("api.client.secret")

  val scopes = config.getStringList("api.scopes").asScala.toList

  val readFileChunkSize = config.getInt("read-file-chunk-size")

}
