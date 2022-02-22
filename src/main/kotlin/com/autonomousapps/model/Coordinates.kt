package com.autonomousapps.model

import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

@JsonClass(generateAdapter = false, generator = "sealed:type")
sealed class Coordinates(
  open val identifier: String
) : Comparable<Coordinates> {

  /**
   * [ProjectCoordinates] come before [ModuleCoordinates], which come before [IncludedBuildCoordinates], which come
   * before [FlatCoordinates].
   */
  override fun compareTo(other: Coordinates): Int {
    return if (this is ProjectCoordinates) {
      if (other !is ProjectCoordinates) 1 else identifier.compareTo(other.identifier)
    } else if (this is ModuleCoordinates) {
      when (other) {
        is ProjectCoordinates -> -1
        is FlatCoordinates -> 1
        is ModuleCoordinates -> identifier.compareTo(other.identifier)
        is IncludedBuildCoordinates -> identifier.compareTo(other.identifier)
      }
    } else {
      if (other !is FlatCoordinates) 1 else gav().compareTo(other.gav())
    }
  }

  /** Group-artifact-version (GAV) string representation, as used in Gradle dependency declarations. */
  abstract fun gav(): String

  fun toFileName() = gav().replace(":", "__") + ".json"

  companion object {
    /** Convert a raw string into [Coordinates]. */
    fun of(raw: String): Coordinates {
      return if (raw.startsWith(':')) {
        ProjectCoordinates(raw)
      } else {
        val c = raw.split(':')
        if (c.size == 3) ModuleCoordinates(identifier = "${c[0]}:${c[1]}", resolvedVersion = c[2])
        else FlatCoordinates(raw)
      }
    }
  }
}

@TypeLabel("project")
@JsonClass(generateAdapter = false)
data class ProjectCoordinates(
  override val identifier: String
) : Coordinates(identifier) {

  init {
    check(identifier.startsWith(':')) { "Project coordinates must start with a ':'" }
  }

  override fun gav(): String = identifier
}

@TypeLabel("module")
@JsonClass(generateAdapter = false)
data class ModuleCoordinates(
  override val identifier: String,
  val resolvedVersion: String
) : Coordinates(identifier) {
  override fun gav(): String = "$identifier:$resolvedVersion"
}

/** For dependencies that have no version information. They might be a flat file on disk, or e.g. "Gradle API". */
@TypeLabel("flat")
@JsonClass(generateAdapter = false)
data class FlatCoordinates(
  override val identifier: String
) : Coordinates(identifier) {
  override fun gav(): String = identifier
}

@TypeLabel("included_build")
@JsonClass(generateAdapter = false)
data class IncludedBuildCoordinates(
  override val identifier: String,
  val requestedVersion: String,
  val resolvedProject: ProjectCoordinates
) : Coordinates(identifier) {
  override fun gav(): String = "$identifier:$requestedVersion"

  companion object {
    fun of(requested: ModuleCoordinates, resolvedProject: ProjectCoordinates) = IncludedBuildCoordinates(
      identifier = requested.identifier,
      requestedVersion = requested.resolvedVersion,
      resolvedProject = resolvedProject
    )
  }
}
