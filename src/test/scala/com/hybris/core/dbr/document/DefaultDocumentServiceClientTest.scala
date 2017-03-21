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
import akka.http.scaladsl.server.Directives.{path, _}
import akka.stream.ActorMaterializer
import com.hybris.core.dbr.BaseCoreTest
import com.hybris.core.dbr.config.BuildInfo
import com.hybris.core.dbr.exceptions.DocumentServiceClientException
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class DefaultDocumentServiceClientTest extends BaseCoreTest {

  import system.dispatcher

  implicit val materializer = ActorMaterializer()

  implicit val defaultPatience = PatienceConfig(timeout = Span(2, Seconds), interval = Span(30, Millis))

  val client = new DefaultDocumentServiceClient("http://localhost:9877", Some("token"))

  "DefaultDocumentBackupClient" when {

    "getting types" should {

      "get types" in {

        val types = client.getTypes("client.token", "getTypesTenant").futureValue

        types must contain theSameElementsAs List("type1", "type2")
      }

      "get indexes" in {
        val indexes = client.getIndexes("client", "getTypesTenant", "type").futureValue

        indexes.size mustBe 2
        indexes.head.hcursor.downField("keys").downField("_id").as[Int] mustBe Right(1)
        indexes.head.hcursor.downField("options").downField("name").as[String] mustBe Right("_id_")

        indexes(1).hcursor.downField("keys").downField("test").as[String] mustBe Right("text")
        indexes(1).hcursor.downField("options").downField("name").as[String] mustBe Right("text")
      }

      "get types with User-Agent header" in {

        val types = client.getTypes("client.useragent", "getTypesTenant").futureValue

        types must contain theSameElementsAs List("type1", "type2")
      }

      "get types without token" in {

        val client = new DefaultDocumentServiceClient("http://localhost:9877", None)

        val types = client.getTypes("client.notoken", "getTypesTenant").futureValue

        types must contain theSameElementsAs List("type1", "type2")
      }

      "handle bad response when getting types" in {

        val result = client.getTypes("client.bad", "getTypesTenant").failed.futureValue

        result mustBe a[DocumentServiceClientException]
        result.asInstanceOf[DocumentServiceClientException].message must include("bad request message")
        result.asInstanceOf[DocumentServiceClientException].message must include("400")
      }

      "handle failed request" in {

        val client = new DefaultDocumentServiceClient("http://localhost:19382", None)

        whenReady(client.getTypes("client.bad", "getTypesTenant").failed) { result ⇒
          result mustBe a[DocumentServiceClientException]
        }
      }
    }

    "creating index" should {

      "create index" in {
        val result = client.createIndex("client", "createIndexTenant", "cats", """{"keys": {"a": 1}}""").futureValue

        result mustBe "newindex"
      }

      "create index with User-Agent header" in {

        val result = client.createIndex("client", "createIndexTenant", "cats.useragent", """{"keys": {"a": 1}}""").futureValue

        result mustBe "newindex"
      }

      "get types without token" in {

        val client = new DefaultDocumentServiceClient("http://localhost:9877", None)

        val types = client.createIndex("client", "createIndexTenant", "cats.notoken", """{"keys": {"a": 1}}""").futureValue

        types mustBe "newindex"
      }

      "handle bad response when getting types" in {

        val result = client.createIndex("client", "createIndexTenant", "cats.bad", """{"keys": {"a": 1}}""").failed.futureValue

        result mustBe a[DocumentServiceClientException]
        result.asInstanceOf[DocumentServiceClientException].message must include("bad request message")
        result.asInstanceOf[DocumentServiceClientException].message must include("400")
      }

      "handle failed request" in {

        val client = new DefaultDocumentServiceClient("http://localhost:19382", None)

        whenReady(client.createIndex("client", "createIndexTenant", "cats.bad", """{"keys": {"a": 1}}""").failed) { result ⇒
          result mustBe a[DocumentServiceClientException]
        }
      }
    }
  }

  def extractToken: PartialFunction[HttpHeader, String] = {
    case Authorization(OAuth2BearerToken(token)) => token
  }

  val createIndexRoute = pathPrefix("createIndexTenant" / "client" / "indexes") {
    post {
      path("cats") {
        headerValuePF(extractToken) { token =>
          entity(as[String]) { entity ⇒
            if (token == "token" && entity == """{"keys": {"a": 1}}""") {
              complete(HttpEntity(ContentTypes.`application/json`, """{"id" : "newindex"}"""))
            } else {
              complete(StatusCodes.BadRequest)
            }
          }
        }
      } ~ path("cats.useragent") {
        headerValueByName("User-Agent") { userAgent =>
          if (userAgent == s"${BuildInfo.name}-${BuildInfo.version}") {
            complete(HttpEntity(ContentTypes.`application/json`, """{"id" : "newindex"}"""))
          }
          else {
            complete(StatusCodes.BadRequest → s"User-Agent in request: '$userAgent'. Expected: ${BuildInfo.name}-${BuildInfo.version}.")
          }
        }
      } ~ path("cats.notoken") {
        headerValueByName("hybris-client") { client =>
          headerValueByName("hybris-tenant") { tenant =>
            (client, tenant) match {
              case ("client", "client") =>
                complete(HttpEntity(ContentTypes.`application/json`, """{"id" : "newindex"}"""))
              case _ ⇒
                complete(StatusCodes.BadRequest)
            }
          }
        }
      } ~
        path("cats.bad") {
          complete((StatusCodes.BadRequest, "bad request message"))
        }
    }
  }

  val getTypesRoute =
    pathPrefix("getTypesTenant") {
      get {
        path("client" / "indexes" / "type") {
          headerValuePF(extractToken) { token =>
            if (token == "token") {
              complete(HttpEntity(ContentTypes.`application/json`, """[{ "keys": { "_id": 1 }, "options": { "name":"_id_" } }, { "keys": { "test": "text" }, "options": { "name":"text" } }]"""))
            } else {
              complete(StatusCodes.BadRequest)
            }
          }
        } ~
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
                  case ("client.notoken", "client") =>
                    complete(HttpEntity(ContentTypes.`application/json`, """{"types" : ["type1", "type2"]}"""))
                  case _ ⇒
                    complete(StatusCodes.BadRequest)
                }
              }
            }
          } ~
          path("client.useragent") {
            headerValueByName("User-Agent") { userAgent =>
              if (userAgent == s"${BuildInfo.name}-${BuildInfo.version}") {
                complete(HttpEntity(ContentTypes.`application/json`, """{"types" : ["type1", "type2"]}"""))
              }
              else {
                complete(StatusCodes.BadRequest → s"User-Agent in request: '$userAgent'. Expected: ${BuildInfo.name}-${BuildInfo.version}.")
              }
            }
          } ~
          path("client.bad") {
            complete((StatusCodes.BadRequest, "bad request message"))
          }
      }
    }

  var binding: ServerBinding = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    val routes = getTypesRoute ~ createIndexRoute
    binding = Await.result(Http().bindAndHandle(routes, "localhost", 9877), 3 seconds)
  }

  override protected def afterAll(): Unit = {
    Await.result(binding.unbind(), 3 seconds)
    super.afterAll()
  }
}
