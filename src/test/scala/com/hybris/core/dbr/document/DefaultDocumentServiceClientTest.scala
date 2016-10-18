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

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpHeader, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.hybris.core.dbr.BaseCoreTest
import com.hybris.core.dbr.exceptions.DocumentServiceClientException
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class DefaultDocumentServiceClientTest extends BaseCoreTest {

  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  implicit val defaultPatience = PatienceConfig(timeout = Span(2, Seconds), interval = Span(30, Millis))

  val client = new DefaultDocumentServiceClient("http://localhost:9876", Some("token"))

  "DefaultDocumentServiceClient" should {

    "get types" in {

      val types = client.getTypes("client.token", "typesTenant").futureValue

      types must contain theSameElementsAs List("type1", "type2")
    }

    "get types without token" in {

      val client = new DefaultDocumentServiceClient("http://localhost:9876", None)

      val types = client.getTypes("client.notoken", "typesTenant").futureValue

      types must contain theSameElementsAs List("type1", "type2")
    }

    "handle bad response when getting types" in {

      val result = client.getTypes("client.bad", "typesTenant").failed.futureValue

      result mustBe a[DocumentServiceClientException]
    }

    "get documents" in {

      val stream = client.getDocuments("client.token", "documentsTenant", "items").futureValue

      val result = stream.map(_.utf8String).runFold("")(_ + _).futureValue

      result mustBe """[{"doc":1},{"doc":2}]"""
    }

    "handle bad response when getting documents" in {

      val result = client.getDocuments("client.bad", "documentsTenant", "items").failed.futureValue

      result mustBe a[DocumentServiceClientException]
    }

    "insert raw document" in {

      val result = client.insertRawDocument("client.token", "insertTenant", "items", """{"a":1}""").futureValue

      result mustBe "someId"
    }

    "handle bad response when inserting raw document" in {

      val result = client.insertRawDocument("client.bad", "insertTenant", "items", """{"a":1}""").failed.futureValue

      result mustBe a[DocumentServiceClientException]
    }
  }

  def extractToken: PartialFunction[HttpHeader, String] = {
    case Authorization(OAuth2BearerToken(token)) => token
  }

  val route =
    pathPrefix("typesTenant") {
      get {
        path("client.token") {
          headerValuePF(extractToken) { token =>
            if (token == "token") {
              complete(HttpEntity(ContentTypes.`application/json`, """{"types" : ["type1", "type2"]}"""))
            } else {
              complete(StatusCodes.BadRequest)
            }
          }
        } ~
          path("client.notoken") {
            headerValueByName("hybris-client") { client =>
              headerValueByName("hybris-tenant") { tenant =>
                (client, tenant) match {
                  case ("client.notoken", "typesTenant") =>
                    complete(HttpEntity(ContentTypes.`application/json`, """{"types" : ["type1", "type2"]}"""))
                  case _ ⇒
                    complete(StatusCodes.BadRequest)
                }
              }
            }
          } ~
          path("client.bad") {
            complete(StatusCodes.BadRequest)
          }
      }
    } ~
      pathPrefix("documentsTenant") {
        get {
          path("client.token" / "data" / "items") {
            headerValuePF(extractToken) { token =>
              if (token == "token") {
                complete(HttpEntity(ContentTypes.`application/json`, """[{"doc":1},{"doc":2}]"""))
              } else {
                complete(StatusCodes.BadRequest)
              }
            }
          } ~
            path("client.bad" / "data" / "items") {
              complete(StatusCodes.BadRequest)
            }
        }
      } ~
      pathPrefix("insertTenant") {
        post {
          path("client.token" / "data" / "items") {
            entity(as[String]) { body =>
              headerValuePF(extractToken) { token =>
                if (body == """{"a":1}""" && token == "token") {
                  complete(HttpEntity(ContentTypes.`application/json`, """{"id" : "someId"}"""))
                } else {
                  complete(StatusCodes.BadRequest)
                }
              }
            }
          } ~
            path("client.bad" / "data" / "items") {
              complete(StatusCodes.BadRequest)
            }
        }
      }

  var binding: ServerBinding = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    binding = Await.result(Http().bindAndHandle(route, "localhost", 9876), 3 seconds)
  }

  override protected def afterAll(): Unit = {
    Await.result(binding.unbind(), 3 seconds)
    super.afterAll()
  }
}
