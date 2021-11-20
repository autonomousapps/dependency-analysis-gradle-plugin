@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapNotNullToSet
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.AndroidResCapability
import com.autonomousapps.model.intermediates.AndroidResDependency
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.io.File

/**
 * This task produces a set of import statements (such as `com.mypackage.R`) for all Android libraries on the compile
 * classpath. These are not necessarily used.
 */
@CacheableTask
abstract class FindAndroidResTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of all R import candidates from set of dependencies"
  }

  private lateinit var androidSymbols: ArtifactCollection

  fun setAndroidSymbols(resources: ArtifactCollection) {
    this.androidSymbols = resources
  }

  /**
   * Artifact type "android-symbol-with-package-name". All Android libraries seem to have this.
   */
  @PathSensitive(PathSensitivity.NAME_ONLY)
  @InputFiles
  fun getAndroidSymbols(): FileCollection = androidSymbols.artifactFiles

  private lateinit var androidPublicRes: ArtifactCollection

  fun setAndroidPublicRes(androidPublicRes: ArtifactCollection) {
    this.androidPublicRes = androidPublicRes
  }

  /**
   * Artifact type "android-public-res". Appears to only be for platform dependencies that bother to include a
   * `public.xml`.
   */
  @PathSensitive(PathSensitivity.NAME_ONLY)
  @InputFiles
  fun getAndroidPublicRes(): FileCollection = androidPublicRes.artifactFiles

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction
  fun action() {
    val outputFile = output.getAndDelete()

    val androidRes: Set<AndroidResDependency> =
      (androidResFrom(androidSymbols) + androidResFrom(androidPublicRes)).toSortedSet()

    outputFile.writeText(androidRes.toJson())
  }

  private fun androidResFrom(artifacts: ArtifactCollection): Set<AndroidResDependency> {
    return artifacts.mapNotNullToSet { resArtifact ->
      try {
        // TODO this could be more efficient. It opens the file twice
        val import = extractResImportFromResFile(resArtifact.file)
        val lines = extractLinesFromRes(resArtifact.file)
        if (import != null) {
          AndroidResDependency(
            coordinates = resArtifact.id.componentIdentifier.toCoordinates(),
            import = import,
            lines = lines
          )
        } else {
          null
        }
      } catch (e: GradleException) {
        null
      }
    }
  }

  private fun extractResImportFromResFile(resFile: File): String? {
    val pn = resFile.useLines { it.firstOrNull() } ?: return null
    return "$pn.R"
  }

  private fun extractLinesFromRes(producerRes: File): List<AndroidResCapability.Line> {
    return producerRes.useLines { lines ->
      lines
        .mapNotNull { line ->
          // First line of file is the package. Every subsequent line is two elements delimited by a space. The first
          // element is the res type (such as "drawable") and the second element is the ID (filename).
          val split = line.split(' ')
          if (split.size == 2) {
            AndroidResCapability.Line(split[0], split[1])
          } else {
            null
          }
        }.toList()
    }
  }
}
