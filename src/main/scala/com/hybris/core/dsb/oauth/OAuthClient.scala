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
package com.hybris.core.dsb.oauth

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import cats.data.XorT
import cats.syntax.xor._
import com.hybris.core.dsb.model.Result.Result
import com.hybris.core.dsb.model.{AppError, InternalAppError}
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.Decoder
import io.circe.generic.semiauto._

import scala.concurrent.{ExecutionContext, Future}

trait OAuth {

  def getToken: Result[String]

}

class OAuthClient(oauthUri: String, clientId: String, clientSecret: String, scopes: List[String])
                 (implicit system: ActorSystem, executionContext: ExecutionContext) extends CirceSupport with OAuth {

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

  def getToken: Result[String] = {
    val result = Http()
      .singleRequest(getTokenRequest)
      .flatMap {
        case response if response.status.isSuccess() ⇒
          Unmarshal(response).to[TokenResponse].map(r ⇒ r.access_token.right[AppError])
        case response ⇒
          Future.successful(InternalAppError(s"Failed to get token: ${response.status} $oauthUri").left)
      }
    XorT(result)
  }
}
