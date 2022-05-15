package com.autonomousapps.model

import com.autonomousapps.internal.utils.toCoordinates
import com.squareup.moshi.JsonClass
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import java.io.File

@JsonClass(generateAdapter = true)
internal data class PhysicalArtifact(
  val coordinates: Coordinates,
  /** Physical artifact on disk; a jar file. */
  val file: File
) : Comparable<PhysicalArtifact> {

  override fun compareTo(other: PhysicalArtifact): Int {
    return coordinates.compareTo(other.coordinates)
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
