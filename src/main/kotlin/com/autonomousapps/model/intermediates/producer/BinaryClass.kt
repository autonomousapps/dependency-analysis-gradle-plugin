// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.intermediates.producer

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.squareup.moshi.JsonClass
import java.util.SortedSet

// TODO: ideally this would be internal. Do the Capabilities really need to be public?
@JsonClass(generateAdapter = false)
data class BinaryClass(
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
