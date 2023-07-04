package com.autonomousapps.model

import com.autonomousapps.internal.kotlin.KotlinPlatformType
import com.squareup.moshi.JsonClass
import java.io.Serializable
import org.gradle.api.artifacts.result.ResolvedVariantResult

@JsonClass(generateAdapter = false)
data class GradleVariantIdentification(
  val capabilities: Set<String>,
  val attributes: Map<String, String>,
  /** Corresponds to [ResolvedVariantResult.getExternalVariant]. */
  val externalVariant: GradleVariantIdentification? = null
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
 * At a general level, these can be thought of as a canonical proxy dependency and "the rest", which
 * covers specific platform targets like [KotlinPlatformType.jvm], [KotlinPlatformType.native], etc.
 *
 * When inspecting attributes for the context of DAGP, we want to know if a given dependency is a canonical
 * dependency or a specific target. When recommending advice, we want to defer to the canonical deps as
 * a sort of implicit bundle of its other targets and will resolve to the correct target.
 */
internal val Coordinates.kmpAttribute get() = gradleVariantIdentification.attributes[KotlinPlatformType.attribute.name]

/**
 * Returns whether this is a canonical KMP dependency, where "canonical" means it is not an alias to an external
 * variant that it's been resolved to.
 */
internal val Coordinates.isKmpCanonicalDependency: Boolean
  get() = kmpAttribute != null && gradleVariantIdentification.externalVariant != null

/** Returns whether this is a specific target KMP dependency, such as [KotlinPlatformType.jvm]. */
internal val Coordinates.isKmpTargetTarget: Boolean
  get() = kmpAttribute != null && gradleVariantIdentification.externalVariant == null

/**
 * Returns a [Coordinates.gav] string that represents the parent [Coordinates.identifier] of the given KMP target's
 * `common` parent.
 */
internal fun ModuleCoordinates.kmpCommonParentIdentifier(): String {
  return identifier.substringBeforeLast('-')
}
