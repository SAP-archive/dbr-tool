/*
 * [y] hybris Platform
 *
 * Copyright (c) 2000-2017 hybris AG
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of hybris
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with hybris.
 */
package com.hybris.core.dbr.model

import io.circe.Json

/**
 * Container for index from Document service.
 *
 * @param keys    index keys
 * @param options index options
 */
case class IndexDefinition(keys: Json, options: Json) {
  val isIdIndexDefinition: Boolean = keys.hcursor.fieldSet.exists(s ⇒ s.forall(_.equals("_id")))
}