// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.squareup.moshi.JsonClass
import java.io.Serializable

@JsonClass(generateAdapter = false)
public data class GradleVariantIdentification(
  val capabilities: Set<String>,
  val attributes: Map<String, String>,
) : Serializable, Comparable<GradleVariantIdentification> {

  public companion object {
    public val EMPTY: GradleVariantIdentification = GradleVariantIdentification(emptySet(), emptyMap())

    internal const val ENFORCED_PLATFORM = "-enforced-platform"
    internal const val PLATFORM = "-platform"
    internal const val TEST_FIXTURES = "-test-fixtures"

    @JvmStatic
    public fun ofCapabilities(vararg capabilities: String): GradleVariantIdentification {
      return GradleVariantIdentification(capabilities.toSet(), emptyMap())
    }
  }

  override fun compareTo(other: GradleVariantIdentification): Int {
    return toSingleString().compareTo(other.toSingleString())
  }

  private fun toSingleString() =
    capabilities.sorted().joinToString() + attributes.map { (k, v) -> "$k=$v" }.sorted().joinToString()

  /**
   * Check that all the requested capabilities are declared in the 'target'. Otherwise, the target can't be a target
   * of the given declarations. The requested capabilities ALWAYS have to exist in a target to be selected.
   */
  internal fun variantMatches(target: Coordinates): Boolean = when (target) {
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
}
