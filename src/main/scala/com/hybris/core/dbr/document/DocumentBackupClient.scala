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

import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.concurrent.Future

/**
  * Client for Document Backup service.
  */
trait DocumentBackupClient {

  /**
    * Returns all documents of given type as a stream of data.
    *
    * @param client client responsible for type
    * @param tenant tenant using type
    * @param `type` name of a type
    * @return future with source of data
    */
  def getDocuments(client: String, tenant: String, `type`: String): Future[Source[ByteString, Any]]

  /**
    * Inserts a stream of documents.
    *
    * @param client    name of a client
    * @param tenant    name of a tenant
    * @param `type`    name of a type
    * @param documents a stream of documents
    * @return future with number of inserted documents
    */
  def insertRawDocuments(client: String, tenant: String, `type`: String, documents: Source[ByteString, _]): Future[Int]

}
