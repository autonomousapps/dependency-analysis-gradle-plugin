package com.autonomousapps.model.declaration

import com.autonomousapps.internal.utils.capitalizeSafely
import org.gradle.api.tasks.SourceSet

/**
 * A "Variant" has two meanings depending on context:
 * 1. For the JVM, it is simply the source set (e.g., "main" and "test").
 * 2. For Android, it is the combination of _variant_ (e.g., "debug" and "release") and [SourceSet] ("main" and
 * "test").
 */
data class Variant(
  val androidVariant: String,
  val sourceSetName: String
) : Comparable<Variant> {

  override fun compareTo(other: Variant): Int = compareBy<Variant> { sourceSetName }
    .thenBy { androidVariant }.compare(this, other)

  /** See [SourceSet.getName]. */
  fun base() = Variant(BASE_VARIANT, sourceSetName)

  @OptIn(ExperimentalStdlibApi::class)
  fun variantSpecificBucketName(bucket: String): String {
    // testDebugImplementation == ${sourceSetName}${androidVariant}${bucket}
    val sourceSetName = if (sourceSetName == SourceSet.MAIN_SOURCE_SET_NAME)
      ""
    else
      sourceSetName

    val androidVariant = if (androidVariant == BASE_VARIANT)
      ""
    else
      androidVariant

    return "${sourceSetName}${androidVariant.replaceFirstChar(Char::uppercase)}${bucket.capitalizeSafely()}"
      .replaceFirstChar(Char::lowercase)
  }

  companion object {
    // In non-Android builds there is only one 'variant' for each source set
    const val BASE_VARIANT = "BASE"

    @JvmStatic
    fun of(configurationName: String): Variant = Configurations.variantFrom(configurationName)

    fun String.toVariant(sourceSetName: String) = Variant(this, sourceSetName)
  }
}
