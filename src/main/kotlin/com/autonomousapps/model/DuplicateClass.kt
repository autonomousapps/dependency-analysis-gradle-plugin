package com.autonomousapps.model

import com.autonomousapps.extension.Behavior
import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.autonomousapps.model.source.SourceKind
import com.squareup.moshi.JsonClass

/**
 * A fully-qualified [className] (`/`-delimited) that is provided by multiple [dependencies], and associated with a
 * [sourceKind].
 */
@JsonClass(generateAdapter = false)
data class DuplicateClass(
  /** The variant (e.g., "main" or "test") associated with this class. */
  // val variant: Variant,
  val sourceKind: SourceKind,
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

  private val dotty = className.replace('/', '.')

  internal fun containsMatchIn(behavior: Behavior): Boolean {
    return behavior.filter.contains(className) || behavior.filter.contains(dotty)
  }

  override fun compareTo(other: DuplicateClass): Int {
    return compareBy(DuplicateClass::sourceKind)
      .thenBy(DuplicateClass::classpathName)
      .thenBy(DuplicateClass::className)
      .thenBy(LexicographicIterableComparator()) { it.dependencies }
      .compare(this, other)
  }
}
