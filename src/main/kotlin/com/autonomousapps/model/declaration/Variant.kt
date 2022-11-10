package com.autonomousapps.model.declaration

import com.squareup.moshi.JsonClass

/**
 * A "Variant" has two meanings depending on context:
 * 1. For the JVM, it is simply the source set (e.g., "main" and "test").
 * 2. For Android, it is the combination of _variant_ (e.g., "debug" and "release") and [SourceSetKind] ("main" and
 * "test").
 */
@JsonClass(generateAdapter = false)
data class Variant(
  val variant: String,
  val kind: SourceSetKind
) : Comparable<Variant> {

  override fun compareTo(other: Variant): Int = compareBy(Variant::kind)
    .thenBy { it.variant }
    .compare(this, other)

  /** See [SourceSetKind.asBaseVariant]. */
  fun base() = kind.asBaseVariant()

  companion object {
    const val MAIN_NAME = "main"
    const val TEST_NAME = "test"
    const val ANDROID_TEST_NAME = "androidTest"

    val MAIN = Variant(MAIN_NAME, SourceSetKind.MAIN)
    //val TEST = Variant(TEST_NAME, SourceSetKind.TEST)
    //val ANDROID_TEST = Variant(ANDROID_TEST_NAME, SourceSetKind.ANDROID_TEST)

    @JvmStatic
    fun of(configurationName: String, supportedSourceSets: Set<String>): Variant? =
      Configurations.variantFrom(configurationName, supportedSourceSets)

    fun String.toVariant(kind: SourceSetKind) = Variant(this, kind)
  }
}
