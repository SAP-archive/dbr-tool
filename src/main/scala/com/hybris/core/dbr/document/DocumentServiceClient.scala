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

import scala.concurrent.Future

/**
  * Client for Document service.
  */
trait DocumentServiceClient {

  /**
    * Returns lists of types for given client and tenant.
    *
    * @param client client responsible for types
    * @param tenant tenant using types
    * @return future with list of types
    */
  def getTypes(client: String, tenant: String): Future[List[String]]

}
