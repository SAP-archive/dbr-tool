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
  * Represents a type: client, tenant and a stream of data.
  *
  * @param client    of the type
  * @param tenant    of the type
  * @param `type`    name
  * @param documents stream of bytestring documents
  */
case class RestoreTypeData(client: String, tenant: String, `type`: String, documents: Source[ByteString, _])
