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
package com.hybris.core.dbr.restore.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import cats.data.Xor
import cats.implicits._
import com.hybris.core.dbr.restore.errors.{InternalServiceError, RestoreError}
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.Decoder
import io.circe.generic.semiauto._

import scala.concurrent.{ExecutionContextExecutor, Future}

final case class Document(id: String)

trait DocumentClient {
  def insertRawDocument(tenant: String, client: String, `type`: String, document: String): Future[Xor[RestoreError, Document]]
}

class DefaultDocumentClient(documentRepositoryUrl: String, credentials: (String, String))(implicit system: ActorSystem)
  extends DocumentClient with CirceSupport {

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  override def insertRawDocument(tenant: String, client: String, `type`: String, document: String): Future[Xor[RestoreError, Document]] = {
    implicit val documentDecoder: Decoder[Document] = deriveDecoder

    val projectUri = s"$documentRepositoryUrl/$tenant/$client/data/${`type`}"

    val uri = Uri(projectUri).withQuery(Query(Map("rawwrite" → "true")))
    val entity = HttpEntity(ContentTypes.`application/json`, document)
    val authorizationHeader = Authorization(BasicHttpCredentials(credentials._1, credentials._2))
    val request = RequestBuilding.Post(uri, entity).withHeaders(authorizationHeader)


    Http().singleRequest(request).flatMap {

      case response if response.status.isSuccess() ⇒
        Unmarshal(response).to[Document].map(_.right)

      case response ⇒
        Future.successful(InternalServiceError(s"Error occurred inserting raw document. Response code: ${response.status}").left)
    }
  }
}
