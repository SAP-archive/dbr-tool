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
package com.hybris.core.dbr.document

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.{Authorization, RawHeader}

trait YaasHeaders {

  def getHeaders(authorizationHeader: Option[Authorization], client: String, tenant: String): List[HttpHeader] = {

    authorizationHeader match {
      case Some(authHeader) ⇒
        authHeader :: Nil
      case None ⇒
        val owner = getClientOwner(client)
        RawHeader("hybris-client", client) ::
          RawHeader("hybris-tenant", tenant) ::
          RawHeader("hybris-client-owner", owner) ::
          Nil
    }
  }

  private def getClientOwner(yaasClient: String) = yaasClient.split('.')(0)
}
