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
package com.hybris.core.dsb.config

import com.typesafe.config.ConfigFactory

/**
 * Application's main configuration, read from application.conf.
 */
trait AppConfig {

  private lazy val config = ConfigFactory.load()

  lazy val summaryFileName = config.getString("summary-file-name")

  def documentUrl(env: String): String = config.getString(s"environments.$env.document-url")

  val documentHttpCredentials = config.getString("document-http-credentials")

}
