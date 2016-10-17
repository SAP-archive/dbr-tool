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

  lazy val appName = config.getString("app.name")
  lazy val appVersion = config.getString("app.version")

  lazy val environments = config.getList("environments.keys").unwrapped().asScala.toList

  lazy val summaryFileName = config.getString("summary-file-name")

  def documentUrl(env: String): String = config.getString(s"environments.$env.document-url")

  // TODO - remove with oauth2
  val documentHttpCredentials = config.getString("document-http-credentials")

}
