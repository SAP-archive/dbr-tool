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
package com.hybris.core.dsb.model

import cats.data.{Xor, XorT}
import cats.instances.future._
import cats.syntax.xor._

import scala.concurrent.{ExecutionContext, Future}

object Result {

  type Result[T] = XorT[Future, AppError, T]

  def success[A](value: A)(implicit ec: ExecutionContext): Result[A] = XorT.fromXor(value.right)

  def failure[A](problem: AppError)(implicit ec: ExecutionContext): Result[A] = XorT.fromXor(problem.left)

  def future[A, B <: AppError](f: Future[Xor[B, A]])(implicit ec: ExecutionContext): Result[A] = XorT(f)

  def apply[A, B <: AppError](f: Future[Xor[B, A]])(implicit ec: ExecutionContext): Result[A] = future(f)
}
