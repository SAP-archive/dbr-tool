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

import akka.NotUsed
import com.hybris.core.dbr.model.IndexDefinition

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
  def getTypes(client: String, tenant: String): Future[Set[String]]

  /**
   * Returns list of indexes for given client, tenant and type.
   *
   * @param client client responsible for types
   * @param tenant tenant using types
   * @param type   type
   * @return
   */
  def getIndexes(client: String, tenant: String, `type`: String): Future[List[IndexDefinition]]

  /**
   * Ensures index exists in given type.
   *
   * @param client     YaaS client
   * @param tenant     YaaS tenant
   * @param `type`     type
   * @param definition of index
   * @return Information that operation has succeed.
   */
  def createIndex(client: String, tenant: String, `type`: String, definition: IndexDefinition): Future[NotUsed]

}
