// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.AndroidResCapability
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.intermediates.AndroidResDependency
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This task produces a set of import statements (such as `com.mypackage.R`) for all Android libraries on the compile
 * classpath. These are not necessarily used.
 */
@CacheableTask
abstract class FindAndroidResTask : DefaultTask() {

  init {
    description = "Produces a report of all R import candidates from set of dependencies"
  }

  private lateinit var androidSymbols: ArtifactCollection

  fun setAndroidSymbols(resources: ArtifactCollection) {
    this.androidSymbols = resources
  }

  /** Artifact type "android-symbol-with-package-name". All Android libraries seem to have this. */
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

    val publicRes = androidResFrom(androidPublicRes, true)
    val allRes = androidResFrom(androidSymbols, false, publicRes.flatMapToSet { it.lines })

    outputFile.bufferWriteJsonSet((allRes + publicRes))
  }

  private fun androidResFrom(
    artifacts: ArtifactCollection,
    isPublicRes: Boolean,
    publicLinesFilter: Set<AndroidResCapability.Line> = emptySet()
  ): Set<AndroidResDependency> {
    return artifacts.mapNotNullToSet { resArtifact ->
      try {
        val (import, lines) = parseResFile(resArtifact.file, isPublicRes, publicLinesFilter)
        if (import != null) {
          AndroidResDependency(
            coordinates = resArtifact.toCoordinates(),
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

  private fun parseResFile(
    resFile: File,
    isPublicRes: Boolean,
    publicLinesFilter: Set<AndroidResCapability.Line>
  ): Pair<String?, List<AndroidResCapability.Line>> {
    var import: String? = null
    val resLines = mutableListOf<AndroidResCapability.Line>()

    val first = AtomicBoolean(true)
    resFile.forEachLine { line ->
      if (first.getAndSet(false)) {
        import = if (isPublicRes) NOT_AN_IMPORT else "$line.R"
      } else {
        // First line of file is the package. Every subsequent line is two elements delimited by a space. The first
        // element is the res type (such as "drawable") and the second element is the ID (filename).
        val split = line.split(' ')
        if (split.size == 2) {
          val resLine = AndroidResCapability.Line(split[0], split[1])
          // This is a convenient way to eliminate false positives in the case an app uses a popular resource from a lib
          // deep in the hierarchy (Theme_AppCompat...) which is included in consumers due to resource merging.
          if (resLine !in publicLinesFilter) {
            resLines += resLine
          }
        }
      }
    }

    return import to resLines
  }

  companion object {
    private const val NOT_AN_IMPORT = "__magic__"

    private operator fun Set<AndroidResDependency>.plus(other: Set<AndroidResDependency>): Set<AndroidResDependency> {
      val sink = mutableMapOf<Coordinates, AndroidResDependency>()
      map { sink[it.coordinates] = it }
      other.map {
        sink.merge(it.coordinates, it) { acc, inc ->
          val import = if (acc.import == NOT_AN_IMPORT) inc.import else acc.import
          check(import != NOT_AN_IMPORT) { "Not an import! ${it.coordinates}." }

          AndroidResDependency(
            coordinates = acc.coordinates,
            import = import,
            // the point
            lines = acc.lines + inc.lines
          )
        }
      }

      return sink.values.toSortedSet()
    }
  }
}
