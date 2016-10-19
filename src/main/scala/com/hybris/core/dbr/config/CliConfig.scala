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

case class CliConfig(command: String = "",
                     env: String = "",
                     client: String = "",
                     configFile: String = "",
                     backupDestinationDir: String = "",
                     restoreSourceDir: String = "") {

  def isBackup: Boolean = command == "backup"

  def isRestore: Boolean = command == "restore"
}
