package com.autonomousapps.model

import com.autonomousapps.internal.utils.toCoordinates
import com.squareup.moshi.JsonClass
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import java.io.File

@JsonClass(generateAdapter = false)
internal data class PhysicalArtifact(
  val coordinates: Coordinates,
  /** Physical artifact on disk; a jar file or directory pointing to class files. */
  val file: File
) : Comparable<PhysicalArtifact> {

  fun isJar(): Boolean = file.name.endsWith(".jar")

  fun containsClassFiles(): Boolean = file.walkBottomUp().any { f -> f.name.endsWith(".class") }

  override fun compareTo(other: PhysicalArtifact): Int {
    return coordinates.compareTo(other.coordinates).let {
      if (it == 0) file.compareTo(other.file) else it
    }
  }

  companion object {
    internal fun of(
      artifact: ResolvedArtifactResult,
      file: File,
    ) = PhysicalArtifact(
      coordinates = artifact.toCoordinates(),
      file = file
    )
  }
}
