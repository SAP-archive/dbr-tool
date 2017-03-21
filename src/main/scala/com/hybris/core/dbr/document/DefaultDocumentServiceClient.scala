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

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.HttpEncodings.identity
import akka.http.scaladsl.model.headers.{`User-Agent`, _}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{Materializer, StreamTcpException}
import com.hybris.core.dbr.config.BuildInfo
import com.hybris.core.dbr.exceptions.DocumentServiceClientException
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, _}

import scala.concurrent.{ExecutionContext, Future}

class DefaultDocumentServiceClient(documentServiceUrl: String,
                                   token: Option[String])
                                  (implicit system: ActorSystem,
                                   materializer: Materializer,
                                   executionContext: ExecutionContext)
  extends DocumentServiceClient with CirceSupport with YaasHeaders {

  private case class GetTypesResponse(types: List[String])
  private implicit val getTypesResponseDecoder: Decoder[GetTypesResponse] = deriveDecoder

  private case class CreateIndexResponse(id: String)
  private implicit val createIndexResponseDecoder: Decoder[CreateIndexResponse] = deriveDecoder

  private val authorizationHeader = token.map(t => Authorization(OAuth2BearerToken(t)))

  override def getTypes(client: String, tenant: String): Future[List[String]] = {

    val request = HttpRequest(
      uri = s"$documentServiceUrl/$tenant/$client",
      headers = `Accept-Encoding`(identity) ::
        `User-Agent`(s"${BuildInfo.name}-${BuildInfo.version}") ::
        getHeaders(authorizationHeader, client, tenant))

    Http()
      .singleRequest(request)
      .flatMap {
        case response if response.status.isSuccess() =>
          Unmarshal(response).to[GetTypesResponse].map(_.types)

        case response =>
          response.entity.dataBytes.runFold(new String)((t, byte) ⇒ t + byte.utf8String).flatMap(msg ⇒
            Future.failed(DocumentServiceClientException(s"Failed to get types for client '$client' and tenant '$tenant'," +
              s". \nStatus code: ${response.status.intValue()}. \nReason: '$msg'."))
          )
      }
      .recoverWith {
        case _: StreamTcpException ⇒
          Future.failed(DocumentServiceClientException(s"TCP error during getting a list of types from the Document service."))
      }
  }

  override def getIndexes(client: String, tenant: String, typeName: String): Future[List[Json]] = {

    val request = HttpRequest(
      uri = s"$documentServiceUrl/$tenant/$client/indexes/$typeName",
      headers = `Accept-Encoding`(identity) ::
        `User-Agent`(s"${BuildInfo.name}-${BuildInfo.version}") ::
        getHeaders(authorizationHeader, client, tenant))

    Http()
      .singleRequest(request)
      .flatMap {
        case response if response.status.isSuccess() =>
          Unmarshal(response).to[List[Json]]

        case response =>
          response.entity.dataBytes.runFold(new String)((t, byte) ⇒ t + byte.utf8String).flatMap(msg ⇒
            Future.failed(DocumentServiceClientException(s"Failed to get indexes for client '$client', tenant '$tenant', and type '$typeName'" +
              s". \nStatus code: ${response.status.intValue()}. \nReason: '$msg'."))
          )
      }
      .recoverWith {
        case _: StreamTcpException ⇒
          Future.failed(DocumentServiceClientException(s"TCP error during getting a list of types from the Document service."))
      }
  }

  override def createIndex(client: String, tenant: String, `type`: String, definition: String): Future[String] = {

    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = s"$documentServiceUrl/$tenant/$client/indexes/${`type`}",
      headers = `Accept-Encoding`(identity) ::
        `User-Agent`(s"${BuildInfo.name}-${BuildInfo.version}") ::
        getHeaders(authorizationHeader, client, tenant))

    Http()
      .singleRequest(request)
      .flatMap {
        case response if response.status.isSuccess() =>
          Unmarshal(response).to[CreateIndexResponse].map(_.id)

        case response =>
          response.entity.dataBytes.runFold(new String)((t, byte) ⇒ t + byte.utf8String).flatMap(msg ⇒ {
            val message = s"Failed to create index for " +
              s"client '$client' and tenant '$tenant' and type '${`type`}'," +
              s". \nStatus code: ${response.status.intValue()}. \nReason: '$msg'."
            Future.failed(DocumentServiceClientException(message))
          }
          )
      }
      .recoverWith {
        case _: StreamTcpException ⇒
          val msg = s"TCP error during creating index for " +
            s"client '$client' and tenant '$tenant' and type '${`type`} " +
            s"in the Document service."
          Future.failed(DocumentServiceClientException(msg))
      }
  }
}
