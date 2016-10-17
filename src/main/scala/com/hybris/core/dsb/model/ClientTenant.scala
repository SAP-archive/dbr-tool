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
package com.hybris.core.dsb.model

/**
 * Input element for backup stream.
 *
 * @param client name of a client
 * @param tenant name of a tenant
 * @param types optional list of types to backup
 */
case class ClientTenant(client: String, tenant: String, types: Option[List[String]])
