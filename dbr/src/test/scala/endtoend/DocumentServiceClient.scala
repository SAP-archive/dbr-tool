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
import com.hybris.core.dbr.oauth.OAuthClient
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

object DocumentServiceClient {

  def apply(baseUrl: String, tenant: String, client: String, `type`: String, clientId: String, clientSecret: String)
           (implicit system: ActorSystem, executionContext: ExecutionContext): DocumentServiceClient = {

    val scopes = List("hybris.document_view hybris.document_manage")

    val token = Await.result(new OAuthClient(s"https://$baseUrl/hybris/oauth2/v1/token",
      clientId,
      clientSecret,
      scopes).getToken, 3 second)

    new DocumentServiceClient(s"https://$baseUrl/hybris/document/v1", token, tenant, client, `type`)
  }

}

class DocumentServiceClient private(documentServiceUrl: String, token: String, tenant: String, client: String, `type`: String)
  extends FailFastCirceSupport {

  private case class InsertDocumentResponse(id: String)

  private implicit val insertDocumentResponse: Decoder[InsertDocumentResponse] = deriveDecoder

  def insertDocument(document: String)
                    (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext): Future[String] = {
    def request(entity: String) = HttpRequest(
      method = POST,
      uri = s"$documentServiceUrl/$tenant/$client/data/${`type`}",
      headers = List(Authorization(OAuth2BearerToken(token))),
      entity = HttpEntity(ContentTypes.`application/json`, entity)
    )

    for {
      response ← Http().singleRequest(request(document))
      entity ← Unmarshal(response).to[InsertDocumentResponse]
    } yield entity.id
  }

  def createIndex(definition: String)
                 (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext): Future[Int] = {
    def request(entity: String) = HttpRequest(
      method = POST,
      uri = s"$documentServiceUrl/$tenant/$client/indexes/${`type`}",
      headers = List(Authorization(OAuth2BearerToken(token))),
      entity = HttpEntity(ContentTypes.`application/json`, entity)
    )

    Http().singleRequest(request(definition)).map(_.status.intValue)
  }

  def getIndex(name: String)
              (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext): Future[Int] = {
    val request = HttpRequest(
      method = GET,
      uri = s"$documentServiceUrl/$tenant/$client/indexes/${`type`}/$name",
      headers = List(Authorization(OAuth2BearerToken(token)))
    )

    Http().singleRequest(request).map(_.status.intValue)
  }

  def getDocument(id: String)
                 (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext): Future[String] = {
    val request = HttpRequest(
      method = GET,
      uri = s"$documentServiceUrl/$tenant/$client/data/${`type`}/$id",
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

  def deleteType(implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext): Future[String] = {
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
