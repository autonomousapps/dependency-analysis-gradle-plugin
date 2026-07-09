// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates.producer

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.autonomousapps.internal.utils.efficient
import com.squareup.moshi.JsonClass
import java.util.*

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

/**
 * Represents a class parsed from bytecode (see `asm.kt`). Includes the [className], the [superClassName] (may be
 * `java/lang/Object`, or `null` if [className] is itself `java/lang/Object`), the set of interfaces (may be empty), and
 * the sets of "effectively public" members (fields and methods that are `public` or `protected`).
 *
 * @see [com.autonomousapps.internal.binary.BinaryCompatibilityChecker]
 * @see [com.autonomousapps.model.internal.intermediates.Reason.BinaryIncompatible]
 */
@JsonClass(generateAdapter = false)
internal data class BinaryClass(
  val className: String,
  val superClassName: String?, // only accessed in BinaryCompatibilityChecker and SuperClassGraphBuilder
  val interfaces: Set<String>, // only accessed in BinaryCompatibilityChecker
  val effectivelyPublicFields: Set<Member.Field>, // only accessed in BinaryCompatibilityChecker and Reason.BinaryIncompatible
  val effectivelyPublicMethods: Set<Member.Method>, // only accessed in BinaryCompatibilityChecker and Reason.BinaryIncompatible
) : Comparable<BinaryClass> {

  override fun compareTo(other: BinaryClass): Int {
    return compareBy(BinaryClass::className)
      .thenComparing(compareBy<BinaryClass, String?>(nullsFirst()) { it.superClassName })
      .thenBy(LexicographicIterableComparator()) { it.interfaces }
      .thenBy(LexicographicIterableComparator()) { it.effectivelyPublicFields }
      .thenBy(LexicographicIterableComparator()) { it.effectivelyPublicMethods }
      .compare(this, other)
  }

  internal class Builder(
    val className: String,
    val superClassName: String?,
    val interfaces: SortedSet<String>,
    val effectivelyPublicFields: SortedSet<Member.Field>,
    val effectivelyPublicMethods: SortedSet<Member.Method>,
  ) {

    fun build(): BinaryClass {
      return BinaryClass(
        className = className.intern(),
        superClassName = superClassName?.intern(),
        interfaces = interfaces.efficient(),
        effectivelyPublicFields = effectivelyPublicFields.efficient(),
        effectivelyPublicMethods = effectivelyPublicMethods.efficient(),
      )
    }
  }
}
