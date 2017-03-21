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
package endtoend

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.{DELETE, GET, POST}
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{HttpEntity, _}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import scala.concurrent.{ExecutionContext, Future}

object DocumentServiceClient extends FailFastCirceSupport {

  private case class InsertDocumentResponse(id: String)

  private implicit val insertDocumentResponse: Decoder[InsertDocumentResponse] = deriveDecoder

  def insertDocument(documentServiceUrl: String, token: String, tenant: String, client: String, `type`: String, document: String)
                    (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext): Future[String] = {
    def getRequest(entity: String) = {
      HttpRequest(
        method = POST,
        uri = s"$documentServiceUrl/$tenant/$client/data/${`type`}",
        headers = List(Authorization(OAuth2BearerToken(token))),
        entity = HttpEntity(ContentTypes.`application/json`, entity))
    }

    for {
      response ← Http().singleRequest(getRequest(document))
      entity ← Unmarshal(response).to[InsertDocumentResponse]
    } yield entity.id
  }

  def getDocument(documentServiceUrl: String, token: String, tenant: String, client: String, `type`: String, documentId: String)
                 (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext): Future[String] = {
    val request = HttpRequest(
      method = GET,
      uri = s"$documentServiceUrl/$tenant/$client/data/${`type`}/$documentId",
      headers = List(Authorization(OAuth2BearerToken(token))))

    Http()
      .singleRequest(request)
      .flatMap {
        case response if response.status.isSuccess() =>
          response.entity.dataBytes.map(_.utf8String).runWith(Sink.head)

        case response ⇒
          println(s"Error during retrieving a document. Code: ${response.status}")
          Future.failed(new RuntimeException("error"))
      }
  }

  def deleteType(documentServiceUrl: String, token: String, tenant: String, client: String, `type`: String)
                (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext): Future[String] = {
    val request = HttpRequest(
      method = DELETE,
      uri = s"$documentServiceUrl/$tenant/$client/data/${`type`}",
      headers = List(Authorization(OAuth2BearerToken(token))))

    Http()
      .singleRequest(request)
      .flatMap {
        case response if response.status.isSuccess() =>
          response.entity.dataBytes.map(_.utf8String).runWith(Sink.head)

        case response ⇒
          println(s"Error during deleting a document. Code: ${response.status}")
          Future.failed(new RuntimeException(s"Error during deleting a document. Code: ${response.status}"))
      }
  }
}
