// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates.producer

import com.squareup.moshi.JsonClass

/**
 * This class exists as a memory optimization. Most of the time, [BinaryClass.effectivelyPublicFields] and
 * [BinaryClass.effectivelyPublicMethods] aren't needed. The only come into play when users have opted-in to the binary
 * compatibility check. In large projects, these properties can dramatically increase heap usage.
 *
 * @see [com.autonomousapps.internal.binary.BinaryCompatibilityChecker]
 * @see [com.autonomousapps.model.internal.intermediates.Reason.BinaryIncompatible]
 */
@JsonClass(generateAdapter = false)
internal data class SimplifiedBinaryClass(
  val className: String,
) : Comparable<SimplifiedBinaryClass> {
  override fun compareTo(other: SimplifiedBinaryClass): Int {
    return compareBy(SimplifiedBinaryClass::className)
      .compare(this, other)
  }
}
