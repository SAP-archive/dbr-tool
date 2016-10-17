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

/**
 * Base element of backup stream.
 *
 * @param client name of a client
 * @param tenant name of a tenant
 * @param `type` name of a type
 */
case class ClientTenantType(client: String, tenant: String, `type`: String)
