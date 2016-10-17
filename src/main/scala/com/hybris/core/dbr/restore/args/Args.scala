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
package com.hybris.core.dbr.restore.args

/**
  * Case class representing input command line arguments.
  * @param env environment
  * @param username to the Document service
  * @param password to the Document service
  * @param config file
  * @param directory containing backups
  */
final case class Args(env: String = "",
                username: String = "",
                password: String = "",
                config: String = "",
                directory: String = "")
