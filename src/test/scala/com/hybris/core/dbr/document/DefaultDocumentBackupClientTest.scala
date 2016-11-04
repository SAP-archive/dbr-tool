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
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.hybris.core.dbr.BaseCoreTest
import com.hybris.core.dbr.exceptions.DocumentBackupClientException
import org.scalatest.time.{Millis, Seconds, Span}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class DefaultDocumentBackupClientTest extends BaseCoreTest {

  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  implicit val defaultPatience = PatienceConfig(timeout = Span(2, Seconds), interval = Span(30, Millis))

  val client = new DefaultDocumentBackupClient("http://localhost:9876", Some("token"))

  "DefaultDocumentBackupClient" should {

    "get documents" in {

      val stream = client.getDocuments("client.token", "documentsTenant", "items").futureValue

      val result = stream.map(_.utf8String).runFold("")(_ + _).futureValue

      result mustBe """[{"doc":1},{"doc":2}]"""
    }

    "handle bad response when getting documents" in {

      val result = client.getDocuments("client.bad", "documentsTenant", "items").failed.futureValue

      result mustBe a[DocumentBackupClientException]
    }

    "insert raw document" in {

      val result = client.insertDocuments("client.token", "insertTenant", "items", Source.single(ByteString("""{"a":1}"""))).futureValue

      result mustBe 1
    }

    "handle bad response when inserting raw document" in {

      val result = client.insertDocuments("client.bad", "insertTenant", "items", Source.single(ByteString("""{"a":1}"""))).failed.futureValue

      result mustBe a[DocumentBackupClientException]
    }
  }

  def extractToken: PartialFunction[HttpHeader, String] = {
    case Authorization(OAuth2BearerToken(token)) => token
  }

  val route =
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
                  complete(HttpEntity(ContentTypes.`application/json`, """{"documentsImported" : "1"}"""))
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
