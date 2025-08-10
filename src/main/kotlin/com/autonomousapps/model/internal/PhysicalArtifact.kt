// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal

import com.autonomousapps.PROJECT_LOGGER
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.Coordinates
import com.squareup.moshi.JsonClass
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import java.io.File

@JsonClass(generateAdapter = false)
internal data class PhysicalArtifact(
  val coordinates: Coordinates,
  /** Physical artifact on disk; a jar file or directory pointing to class files. */
  val file: File,
) : Comparable<PhysicalArtifact> {

  enum class Mode {
    ZIP,
    CLASSES
  }

  init {
    check(isJar() || containsClassFiles()) {
      "'file' must either be a jar or a directory that contains class files. Was '$file'"
    }
  }

  val mode: Mode = if (isJar()) Mode.ZIP else Mode.CLASSES

  fun isJar(): Boolean = isJar(file)
  fun containsClassFiles(): Boolean = containsClassFiles(file)

  override fun compareTo(other: PhysicalArtifact): Int {
    return coordinates.compareTo(other.coordinates).let {
      if (it == 0) file.compareTo(other.file) else it
    }
  }

  companion object {
    internal fun of(
      artifact: ResolvedArtifactResult,
      file: File,
    ): PhysicalArtifact? {
      if (!isValidArtifact(file)) {
        PROJECT_LOGGER.debug(
          "$artifact is not valid as a PhysicalArtifact. $file is neither a jar nor a class-files-containing directory"
        )
        return null
      }

      return PhysicalArtifact(
        coordinates = artifact.toCoordinates(),
        file = file
      )
    }

    /**
     * The [ArtifactCollection][org.gradle.api.artifacts.ArtifactCollection] in
     * [ArtifactsReportTask][com.autonomousapps.tasks.ArtifactsReportTask.compileArtifacts] sometimes contains empty
     * directories from Gradle transforms, and these are not valid as [PhysicalArtifact]s.
     */
    private fun isValidArtifact(file: File): Boolean = isJar(file) || containsClassFiles(file)

    private fun isJar(file: File): Boolean = file.name.endsWith(".jar")
    private fun containsClassFiles(file: File): Boolean = file.walkBottomUp().any { f -> f.name.endsWith(".class") }
  }
}
