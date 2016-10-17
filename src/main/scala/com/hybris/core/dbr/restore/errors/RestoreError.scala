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
package com.hybris.core.dbr.restore.errors

/**
  * Trait representing error during restoration.
  * @param message of an error
  */
sealed abstract class RestoreError(message: String) {
  def getMessage: String = message
}

final case class ParseError(message: String) extends RestoreError(message)

final case class InternalServiceError(message: String) extends RestoreError(message)
