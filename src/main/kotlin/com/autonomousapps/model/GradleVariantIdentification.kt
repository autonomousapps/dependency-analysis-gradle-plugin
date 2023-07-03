package com.autonomousapps.model

import com.autonomousapps.internal.kotlin.KotlinPlatformType
import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = false)
data class GradleVariantIdentification(
  val capabilities: Set<String>,
  val attributes: Map<String, String>
  // classifier: String
): Serializable, Comparable<GradleVariantIdentification> {
  override fun compareTo(other: GradleVariantIdentification): Int {
    return toSingleString().compareTo(other.toSingleString())
  }

  private fun toSingleString() =
    capabilities.sorted().joinToString() + attributes.map { (k, v) -> "$k=$v" }.sorted().joinToString()

  /**
   * Check that all the requested capabilities are declared in the 'target'. Otherwise, the target can't be a target
   * of the given declarations. The requested capabilities ALWAYS have to exist in a target to be selected.
   */
  fun variantMatches(target: Coordinates): Boolean = when(target) {
    is FlatCoordinates -> true
    is ProjectCoordinates -> if (capabilities.isEmpty()) {
      target.gradleVariantIdentification.capabilities.any {
        // If empty, needs to contain the 'default' capability (for projects we need to check with endsWith)
        it.endsWith(target.identifier.substring(target.identifier.lastIndexOf(":")))
      }
    } else {
      target.gradleVariantIdentification.capabilities.containsAll(capabilities)
    }
    else -> if (capabilities.isEmpty()) {
      target.gradleVariantIdentification.capabilities.any {
        // If empty, needs to contain the 'default' capability
        it == target.identifier
      }
    } else {
      target.gradleVariantIdentification.capabilities.containsAll(capabilities)
    }
  }

  companion object {
    val EMPTY = GradleVariantIdentification(emptySet(), emptyMap())
  }
}

/**
 * KMP artifacts have a [KotlinPlatformType] attribute defined for which target it is.
 *
 * At a general level, these can be thought of as [KotlinPlatformType.common] and "the rest", which
 * covers specific platform targets like [KotlinPlatformType.jvm], [KotlinPlatformType.native], etc.
 *
 * When inspecting attributes for the context of DAGP, we want to know if a given dependency is a common
 * dependency or a specific target. When recommending advice, we want to defer to the common targets as
 * a sort of implicit bundle of its other targets.
 */
internal val Coordinates.kmpAttribute get() = gradleVariantIdentification.attributes[KotlinPlatformType.attribute.name]

/** Returns whether this is a [KotlinPlatformType.common] dependency. */
internal val Coordinates.isKmpCommonTarget: Boolean
  get() {
    return when (kmpAttribute) {
      KotlinPlatformType.common.name -> true
      KotlinPlatformType.androidJvm.name -> !isAndroidXAndroidJvmKmpArtifact()
      else -> false
    }
  }

/** Returns whether this is a specific non-common [KotlinPlatformType] dependency, such as [KotlinPlatformType.jvm]. */
internal val Coordinates.isKmpNonCommonTarget: Boolean
  get() {
    return when (kmpAttribute) {
      null -> false
      KotlinPlatformType.common.name -> false
      KotlinPlatformType.androidJvm.name -> isAndroidXAndroidJvmKmpArtifact()
      else -> true
    }
  }

// AndroidX does a really annoying thing in KMP where their "common"
// artifacts are _also_ androidJvm
private fun Coordinates.isAndroidXAndroidJvmKmpArtifact() =
  kmpAttribute == KotlinPlatformType.androidJvm.name &&
    identifier.startsWith("androidx.") && identifier.endsWith("-android")

/**
 * Returns a [Coordinates.gav] string that represents the parent [Coordinates.identifier] of the given KMP target's
 * `common` parent.
 */
internal fun ModuleCoordinates.kmpCommonParentIdentifier(): String {
  return identifier.substringBeforeLast('-')
}
