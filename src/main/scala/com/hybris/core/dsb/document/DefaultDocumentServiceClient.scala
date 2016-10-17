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
package com.hybris.core.dsb.document

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken, RawHeader}
import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import scala.concurrent.{ExecutionContext, Future}


class DefaultDocumentServiceClient(documentServiceUrl: String,
                                   token: Option[String])
                                  (implicit system: ActorSystem,
                                   materializer: Materializer,
                                   executionContext: ExecutionContext)
  extends DocumentServiceClient
    with CirceSupport {

  private case class GetTypesResponse(types: List[String])

  private implicit val getTypesResponseDecoder: Decoder[GetTypesResponse] = deriveDecoder

  override def getTypes(client: String, tenant: String): Future[List[String]] = {

    val request = HttpRequest(uri = s"$documentServiceUrl/$tenant/$client",
      headers = getAuthHeaders(token, client, tenant))

    Http()
      .singleRequest(request)
      .flatMap {
        case response if response.status.isSuccess() =>
          Unmarshal(response).to[GetTypesResponse].map(_.types)
        case response =>
          response.discardEntityBytes()
          Future.failed(new RuntimeException(
            s"Failed to get types for client '$client' and tenant '$tenant'," +
              s" status: ${response.status.intValue()}"))
      }
  }

  override def getDocuments(client: String, tenant: String, `type`: String): Future[Source[ByteString, Any]] = {

    val request = HttpRequest(uri = s"$documentServiceUrl/$tenant/$client/data/${`type`}?fetchAll=true",
      headers = getAuthHeaders(token, client, tenant))

    Http()
      .singleRequest(request)
      .flatMap {
        case response if response.status.isSuccess() =>
          Future.successful(response.entity.dataBytes)
        case response =>
          response.discardEntityBytes()
          Future.failed(new RuntimeException(
            s"Failed to get documents for client '$client',tenant '$tenant'" +
              s" and type '${`type`}', status: ${response.status.intValue()}"))
      }
  }

  private def getAuthHeaders(token: Option[String], client: String, tenant: String): List[HttpHeader] = {
    val hybrisHeaders = List(RawHeader("hybris-client", client), RawHeader("hybris-tenant", tenant))

    token match {

      case Some(t) ⇒ Authorization(OAuth2BearerToken(t)) :: hybrisHeaders

      case None ⇒ hybrisHeaders
    }
  }
}
