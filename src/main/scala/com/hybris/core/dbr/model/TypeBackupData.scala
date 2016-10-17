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
package com.hybris.core.dbr.model

import akka.stream.scaladsl.Source
import akka.util.ByteString

/**
 * Type's data to backup. Contains stream of data from Document service.
 *
 * @param client name of a client
 * @param tenant name of a tenant
 * @param `type` name of a type
 * @param data stream of type's data (document)
 */
case class TypeBackupData(client: String, tenant: String, `type`: String, data: Source[ByteString, Any])
