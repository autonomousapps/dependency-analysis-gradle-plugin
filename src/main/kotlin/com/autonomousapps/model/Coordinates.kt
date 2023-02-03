package com.autonomousapps.model

import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

@JsonClass(generateAdapter = false, generator = "sealed:type")
sealed class Coordinates(
  open val identifier: String,
  open val capability: String
) : Comparable<Coordinates> {

  /**
   * [ProjectCoordinates] come before [ModuleCoordinates], which come before [IncludedBuildCoordinates], which come
   * before [FlatCoordinates].
   */
  override fun compareTo(other: Coordinates): Int {
    return (
      if (this is ProjectCoordinates) {
        if (other is ProjectCoordinates) identifier.compareTo(other.identifier) else 1
      } else if (this is FlatCoordinates) {
        if (other is ProjectCoordinates) -1 else if (other is FlatCoordinates) gav().compareTo(other.gav()) else 1
      } else {
        when (other) {
          is ProjectCoordinates -> -1
          is FlatCoordinates -> -1
          is ModuleCoordinates -> identifier.compareTo(other.identifier)
          is IncludedBuildCoordinates -> identifier.compareTo(other.identifier)
        }
      }).let {
        // after identifiers, compare capabilities
        if (it == 0) {
          capability.compareTo(other.capability)
        } else
          it
      }
  }

  /** Group-artifact-version (GAV) string representation, as used in Gradle dependency declarations. */
  abstract fun gav(): String

  fun toFileName() = gav().replace(":", "__") + ".json"

  companion object {
    /** Convert a raw string into [Coordinates]. */
    fun of(raw: String): Coordinates {
      return if (raw.startsWith(':')) {
        // Note: the second argument (capability) is wrong like this. Where is this code path needed?
        ProjectCoordinates(raw, raw)
      } else {
        val c = raw.split(':')
        if (c.size == 3) {
          val identifier = "${c[0]}:${c[1]}"
          ModuleCoordinates(
            identifier = identifier,
            resolvedVersion = c[2],
            capability = identifier
          )
        } else FlatCoordinates(raw)
      }
    }

    internal fun Coordinates.copy(identifier: String, capability: String): Coordinates = when (this) {
      is ProjectCoordinates -> copy(identifier = identifier, capability = capability)
      is ModuleCoordinates -> copy(identifier = identifier, capability = capability)
      is FlatCoordinates -> copy(identifier = identifier, capability = capability)
      is IncludedBuildCoordinates -> copy(identifier = identifier, capability = capability)
    }
  }
}

@TypeLabel("project")
@JsonClass(generateAdapter = false)
data class ProjectCoordinates(
  override val identifier: String,
  override val capability: String
) : Coordinates(identifier, capability) {

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
  override val capability: String = identifier
) : Coordinates(identifier, capability) {
  override fun gav(): String = "$identifier:$resolvedVersion"
}

/** For dependencies that have no version information. They might be a flat file on disk, or e.g. "Gradle API". */
@TypeLabel("flat")
@JsonClass(generateAdapter = false)
data class FlatCoordinates(
  override val identifier: String
) : Coordinates(identifier, "") { // flat (file) coordinates do not have a capability - it's the empty string
  override fun gav(): String = identifier
}

@TypeLabel("included_build")
@JsonClass(generateAdapter = false)
data class IncludedBuildCoordinates(
  override val identifier: String,
  val resolvedProject: ProjectCoordinates,
  override val capability: String
) : Coordinates(identifier, capability) {
  override fun gav(): String = identifier

  companion object {
    fun of(requested: ModuleCoordinates, resolvedProject: ProjectCoordinates) = IncludedBuildCoordinates(
      identifier = requested.identifier,
      resolvedProject = resolvedProject,
      capability = requested.capability
    )
  }
}

internal class CoordinatesContainer(val coordinates: Set<Coordinates>)
