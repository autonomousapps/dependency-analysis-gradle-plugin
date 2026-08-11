// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal

import com.autonomousapps.PROJECT_LOGGER
import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.autonomousapps.internal.utils.reallyAll
import com.autonomousapps.internal.utils.sequenceOfClassFiles
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.Coordinates
import com.squareup.moshi.JsonClass
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import java.io.File

@JsonClass(generateAdapter = false)
internal data class PhysicalArtifact(
  val coordinates: Coordinates,
  /**
   * Physical artifact on disk; a jar file or directory pointing to class files. This file has an absolute path.
   * nb: attempts to make this file relative have thus far been doomed to fail. Please stop trying.
   */
  val files: Set<File>,
) : Comparable<PhysicalArtifact> {

  enum class Mode {
    ZIP,
    CLASSES
  }

  init {
    check(isJar() || containsClassFiles()) {
      "'files' must either be a jar or a 1+ directories that contains class files. Was '$files'"
    }
  }

  val mode: Mode = if (isJar()) Mode.ZIP else Mode.CLASSES

  fun isJar(): Boolean = isJar(files)
  fun containsClassFiles(): Boolean = containsClassFiles(files)

  fun jarFile(): File {
    require(isJar()) { "Expected jar file. Was '$files'." }
    return files.single()
  }

  fun classFiles(): Sequence<File> {
    require(containsClassFiles()) { "Expected directory(ies) containing class files. Was '$files'." }
    return sequenceOfClassFiles(files)
  }

  fun cacheKey(): String {
    return files.joinToString(separator = ",") { it.absolutePath }
  }

  override fun compareTo(other: PhysicalArtifact): Int {
    return compareBy<PhysicalArtifact>(PhysicalArtifact::coordinates)
      .thenBy(LexicographicIterableComparator()) { it.files }
      .compare(this, other)
  }

  companion object {
    internal fun of(
      artifact: ResolvedArtifactResult,
      files: Set<File>,
    ): PhysicalArtifact? {
      if (!isValidArtifact(files)) {
        PROJECT_LOGGER.debug(
          "{} is not valid as a PhysicalArtifact. {} is neither a jar nor a class-files-containing directory",
          artifact,
          files
        )
        return null
      }

      return PhysicalArtifact(
        coordinates = artifact.toCoordinates(),
        files = files,
      )
    }

    /**
     * The [ArtifactCollection][org.gradle.api.artifacts.ArtifactCollection] in
     * [ArtifactsReportTask][com.autonomousapps.tasks.ArtifactsReportTask.artifacts] sometimes contains empty
     * directories from Gradle transforms, and these are not valid as [PhysicalArtifact]s.
     */
    private fun isValidArtifact(files: Set<File>): Boolean = isJar(files) || containsClassFiles(files)

    private fun isJar(files: Set<File>): Boolean {
      return files.size == 1 && files.single().name.endsWith(".jar")
    }

    private fun containsClassFiles(files: Set<File>): Boolean {
      return files.reallyAll {
        it.walkBottomUp().any { f -> f.name.endsWith(".class") }
      }
    }
  }
}
