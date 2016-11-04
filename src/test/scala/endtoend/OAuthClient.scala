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
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.FormData
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.Decoder
import io.circe.generic.semiauto._

import scala.concurrent.{ExecutionContext, Future}

object OAuthClient extends CirceSupport with LazyLogging {

  case class TokenResponse(access_token: String)

  private implicit val tokenResponseDecoder: Decoder[TokenResponse] = deriveDecoder

  def getToken(oauthUri: String, clientId: String, clientSecret: String, scopes: List[String])
              (implicit system: ActorSystem, executionContext: ExecutionContext, mat: Materializer): Future[String] = {

    val getTokenRequest = RequestBuilding.Post(uri = oauthUri,
      entity = FormData(
        "client_id" → clientId,
        "client_secret" → clientSecret,
        "grant_type" → "client_credentials",
        "scope" → scopes.mkString(" ")
      ).toEntity)

    Http()
      .singleRequest(getTokenRequest)
      .flatMap {
        case response if response.status.isSuccess() ⇒
          Unmarshal(response).to[TokenResponse].map(r ⇒ r.access_token)

        case response ⇒
          logger.error(s"Failed to get token: ${response.status} $oauthUri")
          Future.failed(new RuntimeException(s"Failed to get token: ${response.status} $oauthUri"))
      }
  }

}
