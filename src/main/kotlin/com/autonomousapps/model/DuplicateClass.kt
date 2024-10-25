package com.autonomousapps.model

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.autonomousapps.model.declaration.Variant
import com.squareup.moshi.JsonClass

/**
 * A fully-qualified [classReference] (`/`-delimited) that is provided by multiple [dependencies], and associated with a
 * [variant].
 */
@JsonClass(generateAdapter = false)
data class DuplicateClass(
  val variant: Variant,
  val classpathName: String,
  val classReference: String,
  val dependencies: Set<Coordinates>,
) : Comparable<DuplicateClass> {

  override fun compareTo(other: DuplicateClass): Int {
    return compareBy(DuplicateClass::variant)
      .thenBy(DuplicateClass::classpathName)
      .thenBy(DuplicateClass::classReference)
      .thenBy(LexicographicIterableComparator()) { it.dependencies }
      .compare(this, other)
  }
}
