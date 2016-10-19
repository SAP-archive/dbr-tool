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
package com.hybris.core.dbr.backup

import akka.Done
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.util.ByteString
import better.files.File
import com.hybris.core.dbr.BaseCoreTest
import com.hybris.core.dbr.document.DocumentServiceClient
import com.hybris.core.dbr.model.{BackupType, BackupTypeData, BackupTypeResult, ClientTenant}
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.{Decoder, Json}

import scala.concurrent.Future

class BackupStreamTest extends BaseCoreTest with BackupStream {

  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  "BackupStream" should {

    "add types when not provided" in {

      val documentServiceClient = stub[DocumentServiceClient]

      (documentServiceClient.getTypes _).when("client1", "tenant1").returns(Future.successful(List("type1", "type2")))
      (documentServiceClient.getTypes _).when("client1", "tenant2").returns(Future.successful(List("type1")))

      val (source, sink) = TestSource.probe[ClientTenant]
        .via(addTypes(documentServiceClient))
        .toMat(TestSink.probe[ClientTenant])(Keep.both)
        .run()

      sink.request(5)

      source.sendNext(ClientTenant("client1", "tenant1", List()))
      source.sendNext(ClientTenant("client1", "tenant2", List()))
      source.sendComplete()

      sink.expectNextUnordered(
        ClientTenant("client1", "tenant1", List("type1", "type2")),
        ClientTenant("client1", "tenant2", List("type1"))
      )
      sink.expectComplete()
    }

    "not get types when already provided" in {

      val documentServiceClient = stub[DocumentServiceClient]

      val (source, sink) = TestSource.probe[ClientTenant]
        .via(addTypes(documentServiceClient))
        .toMat(TestSink.probe[ClientTenant])(Keep.both)
        .run()

      sink.request(5)

      source.sendNext(ClientTenant("client1", "tenant1", List("type1", "type2")))
      source.sendNext(ClientTenant("client1", "tenant2", List("type1")))
      source.sendComplete()

      sink.expectNextUnordered(
        ClientTenant("client1", "tenant1", List("type1", "type2")),
        ClientTenant("client1", "tenant2", List("type1"))
      )
      sink.expectComplete()

      (documentServiceClient.getTypes _).verify(*, *).never()
    }

    "flatten types" in {

      val (source, sink) = TestSource.probe[ClientTenant]
        .via(flattenTypes)
        .toMat(TestSink.probe[BackupType])(Keep.both)
        .run()

      sink.request(5)

      source.sendNext(ClientTenant("client1", "tenant1", List("type1", "type2")))
      source.sendNext(ClientTenant("client1", "tenant2", List()))
      source.sendNext(ClientTenant("client1", "tenant4", List("type1")))
      source.sendComplete()

      sink.expectNext(BackupType("client1", "tenant1", "type1"))
      sink.expectNext(BackupType("client1", "tenant1", "type2"))
      sink.expectNext(BackupType("client1", "tenant4", "type1"))
      sink.expectComplete()
    }

    "add documents" in {

      val documentServiceClient = stub[DocumentServiceClient]

      val stream1 = Source(List("a", "b", "c")).map(ByteString(_))
      val stream2 = Source(List("a", "b", "c")).map(ByteString(_))

      (documentServiceClient.getDocuments _).when("client1", "tenant1", "type1")
        .returns(Future.successful(stream1))
      (documentServiceClient.getDocuments _).when("client1", "tenant1", "type2")
        .returns(Future.successful(stream2))

      val (source, sink) = TestSource.probe[BackupType]
        .via(addDocuments(documentServiceClient))
        .toMat(TestSink.probe[BackupTypeData])(Keep.both)
        .run()

      sink.request(5)

      source.sendNext(BackupType("client1", "tenant1", "type1"))
      source.sendNext(BackupType("client1", "tenant1", "type2"))
      source.sendComplete()

      sink.expectNextUnordered(
        BackupTypeData("client1", "tenant1", "type1", stream1),
        BackupTypeData("client1", "tenant1", "type2", stream2)
      )
      sink.expectComplete()
    }

    "write documents to file" in {
      val path = File.newTemporaryDirectory().pathAsString

      val stream = Source(List("a", "b", "c")).map(ByteString(_))

      val (source, sink) = TestSource.probe[BackupTypeData]
        .via(writeToFiles(path))
        .toMat(TestSink.probe[BackupTypeResult])(Keep.both)
        .run()

      sink.request(5)

      source.sendNext(BackupTypeData("client1", "tenant1", "type1", stream))
      source.sendComplete()

      val file = sink.expectNextPF[String] {
        case BackupTypeResult("client1", "tenant1", "type1", f) => f
      }

      File(s"$path/$file").contentAsString mustBe "abc"
    }

    "write summary to file" in {
      val path = File.newTemporaryDirectory().pathAsString

      val (source, sink) = TestSource.probe[BackupTypeResult]
        .via(writeSummary(path, "summary.json"))
        .toMat(TestSink.probe[Done])(Keep.both)
        .run()

      sink.request(5)

      source.sendNext(BackupTypeResult("client1", "tenant1", "type1", "file1"))
      source.sendNext(BackupTypeResult("client1", "tenant1", "type2", "file2"))
      source.sendNext(BackupTypeResult("client1", "tenant2", "type1", "file3"))
      source.sendComplete()

      sink.expectNext(Done)
      sink.expectComplete()

      implicit val resultDecoder: Decoder[BackupTypeResult] = deriveDecoder

      val summary = File(s"$path/summary.json").contentAsString
      val result = parse(summary).getOrElse(Json.Null).as[List[BackupTypeResult]].getOrElse(List())

      result must contain theSameElementsAs List(
        BackupTypeResult("client1", "tenant1", "type1", "file1"),
        BackupTypeResult("client1", "tenant1", "type2", "file2"),
        BackupTypeResult("client1", "tenant2", "type1", "file3")
      )
    }
  }

}
