package com.autonomousapps.model

import com.autonomousapps.internal.utils.toCoordinates
import org.gradle.api.artifacts.component.ComponentIdentifier
import java.io.File

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
      componentIdentifier: ComponentIdentifier,
      file: File,
    ) = PhysicalArtifact(
      coordinates = componentIdentifier.toCoordinates(),
      file = file
    )
  }
}
