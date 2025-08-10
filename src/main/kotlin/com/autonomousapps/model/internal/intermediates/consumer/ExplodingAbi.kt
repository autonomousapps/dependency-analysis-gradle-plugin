// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates.consumer

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
internal data class ExplodingAbi(
  val className: String,
  val sourceFile: String?,
  /** Every class discovered in the bytecode of [className], and which is exposed as part of the ABI. */
  val exposedClasses: Set<String>,
) : Comparable<ExplodingAbi> {
  override fun compareTo(other: ExplodingAbi): Int = className.compareTo(other.className)
}
