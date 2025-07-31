// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates.producer

import com.squareup.moshi.JsonClass

/**
 * An effectively public (public or protected) static final field.
 *
 * @see [com.autonomousapps.model.internal.intermediates.consumer.LdcConstant]
 */
@JsonClass(generateAdapter = false)
internal data class Constant(
  val name: String,
  val descriptor: String,
  val value: String,
) : Comparable<Constant> {

  override fun compareTo(other: Constant): Int {
    return compareBy(Constant::name)
      .thenBy(Constant::descriptor)
      .thenBy { it.value }
      .compare(this, other)
  }
}
