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

import io.circe.Json

/**
  * Represents a collection of restore definitions.
  *
  * @param definitions list of restore definitions to be restored.
  */
case class RestoreDefinition(definitions: List[RestoreTypeDefinition])

/**
  * Single restore definition.
  *
  * @param client to be restored.
  * @param tenant to be restored.
  * @param `type` to be restored.
  * @param file with data to be restored.
  * @param indexes definitions to be restored.
  */
case class RestoreTypeDefinition(client: String, tenant: String, `type`: String, file: String, indexes: Option[List[Json]])
