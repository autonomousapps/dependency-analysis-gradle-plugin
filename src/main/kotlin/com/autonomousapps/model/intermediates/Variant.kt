package com.autonomousapps.model.intermediates

import com.autonomousapps.model.SourceSetKind

/**
 * A "Variant" has two meanings depending on context:
 * 1. For the JVM, it is simply the source set (e.g., "main" and "test").
 * 2. For Android, it is the combination of _variant_ (e.g., "debug" and "release") and [SourceSetKind] ("main" and
 * "test").
 */
data class Variant(
  val variant: String,
  val kind: SourceSetKind
) : Comparable<Variant> {

  override fun compareTo(other: Variant): Int = compareBy(Variant::kind)
    .thenBy { it.variant }
    .compare(this, other)

  fun base() = kind.asBaseVariant()

  @Suppress("MemberVisibilityCanBePrivate")
  companion object {
    const val VARIANT_NAME_MAIN = "main"
    const val VARIANT_NAME_TEST = "test"

    val MAIN = Variant(VARIANT_NAME_MAIN, SourceSetKind.MAIN)
    val TEST = Variant(VARIANT_NAME_TEST, SourceSetKind.MAIN)

    fun String.toVariant(kind: SourceSetKind) = Variant(this, kind)
  }
}
