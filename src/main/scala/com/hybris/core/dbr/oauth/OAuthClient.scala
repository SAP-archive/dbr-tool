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
package com.hybris.core.dbr.oauth

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, StreamTcpException}
import com.hybris.core.dbr.exceptions.OAuthClientException
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Decoder
import io.circe.generic.semiauto._

import scala.concurrent.{ExecutionContext, Future}

trait OAuth {

  /**
   * Accesses the OAuth service and gets valid token.
   *
   * @return token.
   */
  def getToken: Future[String]

}

class OAuthClient(oauthUri: String, clientId: String, clientSecret: String, scopes: List[String])
                 (implicit system: ActorSystem, executionContext: ExecutionContext) extends FailFastCirceSupport with OAuth {

  case class TokenResponse(token_type: String, access_token: String, expires_in: Int, scope: String)

  private implicit val tokenResponseDecoder: Decoder[TokenResponse] = deriveDecoder
  private implicit val mat = ActorMaterializer()

  private val getTokenRequest = RequestBuilding.Post(uri = oauthUri,
    entity = FormData(
      "client_id" → clientId,
      "client_secret" → clientSecret,
      "grant_type" → "client_credentials",
      "scope" → scopes.mkString(" ")
    ).toEntity)

  def getToken: Future[String] =
    validateClientCredentials().flatMap(_ ⇒
      Http()
        .singleRequest(getTokenRequest)
        .flatMap {
          case response if response.status.isSuccess() ⇒
            Unmarshal(response).to[TokenResponse].map(r ⇒ r.access_token)

          case response ⇒
            Future.failed(OAuthClientException(s"Error response from OAuth [${response.status}] $oauthUri"))
        }
        .recoverWith {
          case _: StreamTcpException ⇒
            Future.failed(OAuthClientException(s"TCP error during connecting to OAuth."))
        })

  private def validateClientCredentials(): Future[Boolean] = {
    if (clientId.trim.isEmpty) {
      Future.failed(OAuthClientException(s"Empty CLIENT_ID environment variable."))
    } else if (clientSecret.trim.isEmpty) {
      Future.failed(OAuthClientException(s"Empty CLIENT_SECRET environment variable."))
    } else {
      Future.successful(true)
    }
  }
}
