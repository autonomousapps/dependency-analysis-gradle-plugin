// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates.consumer

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
internal data class ExplodingAbi(
  val className: String,
  val sourceFile: String?,
  val kmpSourceSetName: String?,
  /** Every class discovered in the bytecode of [className], and which is exposed as part of the ABI. */
  val exposedClasses: Set<String>,
) : Comparable<ExplodingAbi> {
  override fun compareTo(other: ExplodingAbi): Int {
    return compareBy<ExplodingAbi>(ExplodingAbi::className)
      .thenComparing(compareBy<ExplodingAbi, String?>(nullsFirst()) { it.sourceFile })
      .thenComparing(compareBy<ExplodingAbi, String?>(nullsFirst()) { it.kmpSourceSetName })
      .thenBy(LexicographicIterableComparator()) { it.exposedClasses }
      .compare(this, other)
  }
}
