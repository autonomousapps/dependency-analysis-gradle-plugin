// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.google.common.hash.Hashing
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import java.nio.charset.StandardCharsets

@JsonClass(generateAdapter = false, generator = "sealed:type")
sealed class Coordinates(
  open val identifier: String,
  open val gradleVariantIdentification: GradleVariantIdentification,
) : Comparable<Coordinates> {

  /**
   * [ProjectCoordinates] come before [ModuleCoordinates], which come before [IncludedBuildCoordinates], which come
   * before [FlatCoordinates].
   */
  override fun compareTo(other: Coordinates): Int {
    return (
      if (this is ProjectCoordinates) {
        if (other is ProjectCoordinates) {
          identifier.compareTo(other.identifier)
        } else {
          1
        }
      } else if (this is ModuleCoordinates) {
        if (other is ProjectCoordinates) {
          -1
        } else if (other is ModuleCoordinates) {
          gav().compareTo(other.gav())
        } else {
          1
        }
      } else if (this is FlatCoordinates) {
        if (other is FlatCoordinates) {
          gav().compareTo(other.gav())
        } else {
          -1
        }
      } else {
        // this is IncludedBuildCoordinates
        when (other) {
          is ProjectCoordinates -> -1
          is ModuleCoordinates -> -1
          is IncludedBuildCoordinates -> identifier.compareTo(other.identifier)
          is FlatCoordinates -> 1
        }
      }).let {
      // after identifiers, compare capabilities
      if (it == 0) {
        gradleVariantIdentification.compareTo(other.gradleVariantIdentification)
      } else
        it
    }
  }

  /** Group-artifact-version (GAV) string representation, as used in Gradle dependency declarations. */
  abstract fun gav(): String

  fun toFileName() = capabilitiesWithoutDefault().let { capabilities ->
    when {
      capabilities.isEmpty() -> "${gav().replace(":", "__")}.json"
      // In case we have capabilities, we use a unique hash for the capability combination in the file name
      // to not mix dependencies with different capabilities.
      else -> "${gav().replace(":", "__")}__${fingerprint(capabilities)}.json"
    }
  }

  private fun fingerprint(capabilities: List<String>) =
    Hashing.fingerprint2011().hashString(capabilities.joinToString("_"), StandardCharsets.UTF_8)

  private fun capabilitiesWithoutDefault() =
    gradleVariantIdentification.capabilities.filter {
      !it.endsWith(identifier) // If empty, needs to contain the 'default' capability
    }.sorted()

  /**
   * In case of an 'ADD' advice, the GradleVariantIdentification is directly sourced from the selected node
   * in the dependency graph. It's hard (or impossible with Gradle's current APIs) to find the exact declaration that
   * let to selecting that node. If we could find that declaration, we could use it for the ADD advice.
   * Right now, we use the details from the node in the Graph which may contain more capabilities as you need
   * to declare. In particular, it also contains the 'default capability', which makes it conceptually equal to
   * Coordinates without capability.
   * In order to correctly reduce advices (e.g. merge a REMOVE and an ADD to a CHANGE), we need the same Coordinates
   * on both. So this method should be used to 'minify' the GradleVariantIdentification for ADD advices.
   *
   * @return A copy of this Coordinates without the 'default capability'
   */
  fun withoutDefaultCapability(): Coordinates {
    return gradleVariantIdentification.capabilities.let { capabilities ->
      when {
        capabilities.size == 1 && isDefaultCapability(capabilities.single(), identifier) -> {
          // Only one capability that is the default -> remove it
          copy(identifier, GradleVariantIdentification.EMPTY)
        }

        capabilities.size > 1 && capabilities.any { isDefaultCapability(it, identifier) } -> {
          // The default capability is in the list, we assume that the others are not important for selection -> remove them all
          copy(identifier, GradleVariantIdentification.EMPTY)
        }

        else -> {
          this
        }
      }
    }
  }

  private fun isDefaultCapability(capability: String, identifier: String) =
    when (this) {
      // For projects, we don't know the 'group' here. We only match the 'name' part and assume that the group fits.
      is ProjectCoordinates -> capability.endsWith(identifier.substring(identifier.lastIndexOf(":")))
      else -> capability == identifier
    }

  companion object {
    /** Convert a raw string into [Coordinates]. */
    fun of(raw: String): Coordinates {
      return if (raw.startsWith(':')) {
        ProjectCoordinates(raw, GradleVariantIdentification.EMPTY)
      } else {
        val c = raw.split(':')
        if (c.size == 3) {
          val identifier = "${c[0]}:${c[1]}"
          ModuleCoordinates(
            identifier = identifier,
            resolvedVersion = c[2],
            gradleVariantIdentification = GradleVariantIdentification.EMPTY
          )
        } else FlatCoordinates(raw)
      }
    }

    internal fun Coordinates.copy(
      identifier: String,
      gradleVariantIdentification: GradleVariantIdentification,
    ): Coordinates = when (this) {
      is ProjectCoordinates -> copy(identifier = identifier, gradleVariantIdentification = gradleVariantIdentification)
      is ModuleCoordinates -> copy(identifier = identifier, gradleVariantIdentification = gradleVariantIdentification)
      is FlatCoordinates -> copy(identifier = identifier)
      is IncludedBuildCoordinates -> copy(
        identifier = identifier,
        gradleVariantIdentification = gradleVariantIdentification
      )
    }
  }
}

@TypeLabel("project")
@JsonClass(generateAdapter = false)
data class ProjectCoordinates(
  override val identifier: String,
  override val gradleVariantIdentification: GradleVariantIdentification,
  val buildPath: String? = null, // Name of the build in a composite for which the project coordinates are valid
) : Coordinates(identifier, gradleVariantIdentification) {

  init {
    check(identifier.startsWith(':')) { "Project coordinates must start with a ':'" }
  }

  override fun gav(): String = identifier
}

@TypeLabel("module")
@JsonClass(generateAdapter = false)
data class ModuleCoordinates(
  override val identifier: String,
  val resolvedVersion: String,
  override val gradleVariantIdentification: GradleVariantIdentification,
) : Coordinates(identifier, gradleVariantIdentification) {
  override fun gav(): String = "$identifier:$resolvedVersion"
}

/** For dependencies that have no version information. They might be a flat file on disk, or e.g. "Gradle API". */
@TypeLabel("flat")
@JsonClass(generateAdapter = false)
data class FlatCoordinates(
  override val identifier: String,
) : Coordinates(identifier, GradleVariantIdentification.EMPTY) {
  override fun gav(): String = identifier
}

@TypeLabel("included_build")
@JsonClass(generateAdapter = false)
data class IncludedBuildCoordinates(
  override val identifier: String,
  val resolvedProject: ProjectCoordinates,
  override val gradleVariantIdentification: GradleVariantIdentification,
) : Coordinates(identifier, gradleVariantIdentification) {
  override fun gav(): String = identifier

  companion object {
    fun of(requested: ModuleCoordinates, resolvedProject: ProjectCoordinates) = IncludedBuildCoordinates(
      identifier = requested.identifier,
      resolvedProject = resolvedProject,
      gradleVariantIdentification = requested.gradleVariantIdentification
    )
  }
}

internal class CoordinatesContainer(val coordinates: Set<Coordinates>)
