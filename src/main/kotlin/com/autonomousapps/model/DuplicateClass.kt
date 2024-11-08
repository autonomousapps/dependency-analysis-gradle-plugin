package com.autonomousapps.model

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.autonomousapps.model.declaration.Variant
import com.squareup.moshi.JsonClass

/**
 * A fully-qualified [className] (`/`-delimited) that is provided by multiple [dependencies], and associated with a
 * [variant].
 */
@JsonClass(generateAdapter = false)
data class DuplicateClass(
  /** The variant (e.g., "main" or "test") associated with this class. */
  val variant: Variant,
  /** The name of the classpath that has this duplication, e.g. "compile" or "runtime". */
  val classpathName: String,
  /** E.g., `java/lang/String`. */
  val className: String,
  /** The set of dependencies that provide this class. */
  val dependencies: Set<Coordinates>,
) : Comparable<DuplicateClass> {

  internal companion object {
    const val COMPILE_CLASSPATH_NAME = "compile"
    const val RUNTIME_CLASSPATH_NAME = "runtime"
  }

  override fun compareTo(other: DuplicateClass): Int {
    return compareBy(DuplicateClass::variant)
      .thenBy(DuplicateClass::classpathName)
      .thenBy(DuplicateClass::className)
      .thenBy(LexicographicIterableComparator()) { it.dependencies }
      .compare(this, other)
  }
}
