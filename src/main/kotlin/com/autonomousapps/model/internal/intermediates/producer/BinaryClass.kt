// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates.producer

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.squareup.moshi.JsonClass
import java.util.SortedSet

/**
 * Represents a class parsed from bytecode (see `asm.kt`). Includes the [className], the [superClassName] (may be
 * `java/lang/Object`), the set of interfaces (may be empty), and the sets of "effectively public" members (fields and
 * methods that are `public` or `protected`).
 *
 * All the class or interface name references are "slashy", not "dotty."
 */
@JsonClass(generateAdapter = false)
internal data class BinaryClass(
  val className: String,
  val superClassName: String,
  val interfaces: Set<String>,
  val effectivelyPublicFields: Set<Member.Field>,
  val effectivelyPublicMethods: Set<Member.Method>,
) : Comparable<BinaryClass> {

  override fun compareTo(other: BinaryClass): Int {
    return compareBy(BinaryClass::className)
      .thenBy(BinaryClass::superClassName)
      .thenBy(LexicographicIterableComparator()) { it.interfaces }
      .thenBy(LexicographicIterableComparator()) { it.effectivelyPublicFields }
      .thenBy(LexicographicIterableComparator()) { it.effectivelyPublicMethods }
      .compare(this, other)
  }

  internal class Builder(
    val className: String,
    val superClassName: String,
    val interfaces: SortedSet<String>,
    val effectivelyPublicFields: SortedSet<Member.Field>,
    val effectivelyPublicMethods: SortedSet<Member.Method>,
  ) {

    fun build(): BinaryClass {
      return BinaryClass(
        className = className,
        superClassName = superClassName,
        interfaces = interfaces,
        effectivelyPublicFields = effectivelyPublicFields,
        effectivelyPublicMethods = effectivelyPublicMethods,
      )
    }
  }
}
