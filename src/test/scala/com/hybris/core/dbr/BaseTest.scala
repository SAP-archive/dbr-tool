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
package com.hybris.core.dbr

import io.circe.Json
import io.circe.parser.parse
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.util.Random

trait BaseTest
  extends WordSpecLike
  with MustMatchers
  with ScalaFutures
  with EitherValues
  with OptionValues
  with MockFactory
  with BeforeAndAfterAll {

  def randomName: String = Random.alphanumeric take 10 mkString

  def toJson(s: String): Json = parse(s).getOrElse(Json.Null)
}
