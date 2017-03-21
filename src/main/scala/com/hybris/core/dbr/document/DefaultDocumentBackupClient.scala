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
import akka.http.scaladsl.model.headers.HttpEncodings.gzip
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{HttpEntity, _}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Compression, Source}
import akka.stream.{Materializer, StreamTcpException}
import akka.util.ByteString
import com.hybris.core.dbr.config.BuildInfo
import com.hybris.core.dbr.exceptions.{DocumentBackupClientException, DocumentServiceClientException}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

import scala.concurrent.{ExecutionContext, Future}

case class InsertResult(totalDocuments: Int, inserted: Int, replaced: Int)

class DefaultDocumentBackupClient(documentBackupUrl: String,
                                  token: Option[String])
                                 (implicit system: ActorSystem,
                                  materializer: Materializer,
                                  executionContext: ExecutionContext)
  extends DocumentBackupClient with FailFastCirceSupport with YaasHeaders {

  val MaxBytesPerChunkDefault: Int = 100 * 1024 * 1024

  private implicit val insertResultDecoder: Decoder[InsertResult] = deriveDecoder

  private val authorizationHeader = token.map(t => Authorization(OAuth2BearerToken(t)))

  override def getDocuments(client: String, tenant: String, `type`: String): Future[Source[ByteString, Any]] = {

    val request = HttpRequest(
      uri = s"$documentBackupUrl/data/$tenant/${`type`}",
      headers = `Accept-Encoding`(gzip) :: `User-Agent`(s"${BuildInfo.name}-${BuildInfo.version}") :: getHeaders(authorizationHeader, client))

    Http()
      .singleRequest(request)
      .flatMap {
        case response if response.status.isSuccess() =>
          Future.successful(getData(response))

        case response =>
          getData(response).runFold(new String)((t, byte) ⇒ t + byte.utf8String).flatMap(msg ⇒
            Future.failed(DocumentBackupClientException(s"Failed to get documents for client '$client',tenant '$tenant'" +
              s" and type '${`type`}'. \nStatus code: ${response.status.intValue()}. \nReason: '$msg'."))
          )
      }
      .recoverWith {
        case _: StreamTcpException ⇒
          Future.failed(DocumentServiceClientException(s"TCP error during getting documents from the Document Backup."))

        case clientException: DocumentBackupClientException ⇒
          Future.failed(clientException)

        case err: Throwable ⇒
          Future.failed(DocumentServiceClientException(s"Getting documents encountered error. Reason: ${err.getMessage}"))
      }
  }

  override def insertDocuments(client: String, tenant: String, `type`: String, documents: Source[ByteString, _]): Future[InsertResult] = {

    val compressedDocuments = documents.via(Compression.gzip)

    val request = HttpRequest(method = HttpMethods.POST,
      uri = s"$documentBackupUrl/data/$tenant/${`type`}",
      entity = HttpEntity(ContentTypes.`application/json`, data = compressedDocuments),
      headers = `Content-Encoding`(gzip) :: `User-Agent`(s"${BuildInfo.name}-${BuildInfo.version}") :: getHeaders(authorizationHeader, client))

    Http()
      .singleRequest(request)
      .flatMap {

        case response if response.status.isSuccess() ⇒
          Unmarshal(response).to[InsertResult]

        case response ⇒
          response.entity.dataBytes.runFold(new String)((t, byte) ⇒ t + byte.utf8String).flatMap(msg ⇒
            Future.failed(DocumentBackupClientException(s"Failed to inserting raw documents. \nStatus code: ${response.status.intValue()}. \nReason: '$msg'"))
          )
      }
      .recoverWith {
        case _: StreamTcpException ⇒
          Future.failed(DocumentBackupClientException(s"TCP error during inserting documents to the Document service."))

        case clientException: DocumentBackupClientException ⇒
          Future.failed(clientException)

        case err: Throwable ⇒
          Future.failed(DocumentBackupClientException(s"Inserting documents encountered error. Reason: ${err.getMessage}"))
      }
  }

  private def getData(response: HttpResponse): Source[ByteString, _] = {
    if (getContentEncoding(response) == gzip.value) {
      response.entity.withoutSizeLimit().dataBytes.via(Compression.gunzip(MaxBytesPerChunkDefault))
    } else {
      response.entity.withoutSizeLimit().dataBytes
    }
  }

  private def getContentEncoding(response: HttpResponse) = {
    response.headers.find(_.name() == `Content-Encoding`.name).map(_.value()).getOrElse(HttpEncodings.identity.value)
  }

}
