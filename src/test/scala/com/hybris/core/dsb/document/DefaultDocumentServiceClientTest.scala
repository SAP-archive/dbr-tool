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

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpHeader, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.hybris.core.dsb.BaseCoreTest
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class DefaultDocumentServiceClientTest extends BaseCoreTest {

  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  implicit val defaultPatience = PatienceConfig(timeout = Span(2, Seconds), interval = Span(30, Millis))

  "DefaultDocumentServiceClient" should {

    "get types" in {

      // given
      val client = new DefaultDocumentServiceClient("http://localhost:9876", None)

      // when
      val types = client.getTypes("client", "tenant1").futureValue

      // then
      types must contain theSameElementsAs List("type1", "type2")

    }

    "get documents" in {

      // given
      val client = new DefaultDocumentServiceClient("http://localhost:9876", None)

      // when
      val stream = client.getDocuments("client", "tenant2", "type1").futureValue

      val result = stream.map(_.utf8String).runFold("")(_ + _).futureValue

      // then
      result mustBe """[{"doc":1},{"doc":2}]"""
    }

    "get types with authorization token" in {

      // given
      val client = new DefaultDocumentServiceClient("http://localhost:9876", Some("token"))

      // when
      val types = client.getTypes("client", "tenant3").futureValue

      // then
      types must contain theSameElementsAs List("type1", "type2")

    }

    "get types without authorization when local mode" in {

      // given
      val client = new DefaultDocumentServiceClient("http://localhost:9876", None)

      // when
      val types = client.getTypes("client", "tenant4").futureValue

      // then
      types must contain theSameElementsAs List("type1", "type2")

    }
  }

  def extractToken: PartialFunction[HttpHeader, String] = {
    case Authorization(OAuth2BearerToken(token)) => token
  }

  val route =
    path("tenant1" / "client") {
      get {
        headerValueByName("hybris-client") { client =>
          headerValueByName("hybris-tenant") { tenant =>
            (client, tenant) match {
              case ("client", "tenant1") =>
                complete(HttpEntity(ContentTypes.`application/json`,
                  """
                    |{
                    |  "types" : ["type1", "type2"]
                    |}
                  """.stripMargin))
              case _ =>
                complete(StatusCodes.BadRequest)
            }
          }
        }
      }
    } ~
      path("tenant2" / "client" / "data" / "type1") {
        get {
          headerValueByName("hybris-client") { client =>
            headerValueByName("hybris-tenant") { tenant =>
              (client, tenant) match {
                case ("client", "tenant2") =>
                  complete(HttpEntity(ContentTypes.`application/json`, """[{"doc":1},{"doc":2}]"""))
                case _ =>
                  complete(StatusCodes.BadRequest)
              }
            }
          }
        }
      } ~
      path("tenant3" / "client") {
        get {
          headerValueByName("hybris-client") { client =>
            headerValueByName("hybris-tenant") { tenant =>
              headerValuePF(extractToken) { token =>
                (client, tenant, token) match {
                  case ("client", "tenant3", "token") =>
                    complete(HttpEntity(ContentTypes.`application/json`,
                      """
                        |{
                        |  "types" : ["type1", "type2"]
                        |}
                      """.
                        stripMargin))
                  case _ =>
                    complete(StatusCodes.
                      BadRequest)
                }
              }
            }
          }
        }
      } ~
      path("tenant4" / "client") {
        get {
          headerValueByName("hybris-client") { client =>
            headerValueByName("hybris-tenant") { tenant =>
              optionalHeaderValueByName("Authorization") { token ⇒
                (client, tenant, token) match {
                  case ("client", "tenant4", None) =>
                    complete(HttpEntity(ContentTypes.`application/json`,
                      """
                        |{
                        |  "types" : ["type1", "type2"]
                        |}
                      """.
                        stripMargin))


                  case _ ⇒
                    complete(StatusCodes.BadRequest)
                }
              }
            }
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
