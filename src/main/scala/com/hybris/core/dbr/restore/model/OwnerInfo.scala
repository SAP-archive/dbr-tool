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
package com.hybris.core.dbr.restore.model

/**
  * Case class representing owner of data.
  * @param client data belongs to
  * @param tenant data belongs to
  * @param `type` data belongs to
  * @param fileName with data
  */
final case class OwnerInfo(client: String, tenant: String, `type`: String, fileName: String)
