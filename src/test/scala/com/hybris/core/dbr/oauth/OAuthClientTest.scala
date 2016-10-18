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

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{ContentTypes, _}
import akka.stream.ActorMaterializer
import cats.scalatest.XorValues
import com.hybris.core.dbr.BaseCoreTest
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


class OAuthClientTest extends BaseCoreTest with XorValues {

  implicit def defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(200, Millis))

  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val successRequestBody = FormData(
    "client_id" → "clientId",
    "client_secret" → "clientSecret",
    "grant_type" → "client_credentials",
    "scope" → ""
  ).toEntity

  val successRequestBodyWithScopes = FormData(
    "client_id" → "clientId",
    "client_secret" → "clientSecret",
    "grant_type" → "client_credentials",
    "scope" → "hybris.media_manage hybris.media_view"
  ).toEntity

  val requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(POST, Uri.Path("/hybris/oauth2/v1/token"), _, body, _) if body.equals(successRequestBody) || body.equals(successRequestBodyWithScopes) =>
      HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`,
        """{
          |  "token_type": "Bearer",
          |  "access_token": "020-b9ea81fd-9518-4fcb-b33d-894c9048dd39",
          |  "expires_in": 3600,
          |  "scope": "hybris.tenant=framefrog"
          |}""".stripMargin))

    case r: HttpRequest =>
      HttpResponse(StatusCodes.NotFound, entity = "Unknown resource!")
  }


  var binding: Http.ServerBinding = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    binding = Await.result(Http().bindAndHandleSync(requestHandler, "localhost", 8999), 1 second)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    Await.result(binding.unbind(), 1 second)
  }

  "OAuthActor" when {
    "tries to get a token" should {
      "succeed with correct response" in {

        val oAuthActor = new OAuthClient("http://localhost:8999/hybris/oauth2/v1/token", "clientId", "clientSecret", List())

        whenReady(oAuthActor.getToken) { result ⇒
          result mustBe "020-b9ea81fd-9518-4fcb-b33d-894c9048dd39"
        }
      }

      "succeed with correct response and scopes" in {

        val oAuthActor = new OAuthClient("http://localhost:8999/hybris/oauth2/v1/token",
          "clientId",
          "clientSecret",
          List("hybris.media_manage", "hybris.media_view"))

        whenReady(oAuthActor.getToken) { result ⇒
          result mustBe "020-b9ea81fd-9518-4fcb-b33d-894c9048dd39"
        }
      }

      "failed with when request response is not 2xx" in {

        val oAuthActor = new OAuthClient("http://localhost:8999/notExistingEndpoint", "clientId", "clientSecret", List())

        whenReady(oAuthActor.getToken.failed) { result ⇒
          result mustBe a[RuntimeException]
        }
      }
    }
  }
}
