// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.Artifact
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.flatMapToSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.internal.AndroidResCapability
import com.autonomousapps.model.internal.intermediates.producer.AndroidResDependency
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * This task produces a set of import statements (such as `com.mypackage.R`) for all Android libraries on the compile
 * classpath. These are not necessarily used.
 */
@CacheableTask
public abstract class FindAndroidResTask : DefaultTask() {

  init {
    description = "Produces a report of all R import candidates from set of dependencies"
  }

  @get:Input
  public abstract val androidSymbolIds: ListProperty<ComponentArtifactIdentifier>

  @get:Input
  public abstract val androidSymbolVariants: ListProperty<ResolvedVariantResult>

  /** Artifact type "android-symbol-with-package-name". All Android libraries seem to have this. */
  @get:PathSensitive(PathSensitivity.NAME_ONLY)
  @get:InputFiles
  public abstract val androidSymbolFiles: ListProperty<File>

  internal fun withAndroidSymbols(artifacts: Provider<Set<ResolvedArtifactResult>>) {
    androidSymbolIds.set(Artifact.ids(artifacts))
    androidSymbolVariants.set(Artifact.variants(artifacts))
    androidSymbolFiles.set(Artifact.files(artifacts))
  }

  @get:Input
  public abstract val androidPublicResIds: ListProperty<ComponentArtifactIdentifier>

  @get:Input
  public abstract val androidPublicResVariants: ListProperty<ResolvedVariantResult>

  /**
   * Artifact type "android-public-res". Appears to only be for platform dependencies that bother to include a
   * `public.xml`.
   */
  @get:PathSensitive(PathSensitivity.NAME_ONLY)
  @get:InputFiles
  public abstract val androidPublicResFiles: ListProperty<File>

  internal fun withAndroidPublicRes(artifacts: Provider<Set<ResolvedArtifactResult>>) {
    androidPublicResIds.set(Artifact.ids(artifacts))
    androidPublicResVariants.set(Artifact.variants(artifacts))
    androidPublicResFiles.set(Artifact.files(artifacts))
  }

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction
  public fun action() {
    val outputFile = output.getAndDelete()

    val publicRes = androidResFrom(androidPublicResIds, androidPublicResVariants, androidPublicResFiles, true)
    val allRes = androidResFrom(
      androidSymbolIds,
      androidSymbolVariants,
      androidSymbolFiles,
      false,
      publicRes.flatMapToSet { it.lines },
    )

    outputFile.bufferWriteJsonSet((allRes + publicRes).toSortedSet())
  }

  private fun androidResFrom(
    ids: ListProperty<ComponentArtifactIdentifier>,
    variants: ListProperty<ResolvedVariantResult>,
    files: ListProperty<File>,
    isPublicRes: Boolean,
    publicLinesFilter: Set<AndroidResCapability.Line> = emptySet()
  ): Set<AndroidResDependency> {
    return Artifact.sequenced(ids, variants, files)
      .mapNotNull { artifact ->
        try {
          val (import, lines) = parseResFile(artifact.file, isPublicRes, publicLinesFilter)
          if (import != null) {
            AndroidResDependency.newInstance(
              coordinates = artifact.toCoordinates(),
              import = import,
              lines = lines,
            )
          } else {
            null
          }
        } catch (_: GradleException) {
          null
        }
      }
      .toSortedSet()
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

  private companion object {
    const val NOT_AN_IMPORT = "__magic__"

    operator fun Set<AndroidResDependency>.plus(other: Set<AndroidResDependency>): Set<AndroidResDependency> {
      val sink = mutableMapOf<Coordinates, AndroidResDependency>()
      forEach { sink[it.coordinates] = it }
      other.forEach {
        sink.merge(it.coordinates, it) { acc, inc ->
          val import = if (acc.import == NOT_AN_IMPORT) inc.import else acc.import
          check(import != NOT_AN_IMPORT) { "Not an import! ${it.coordinates}." }

          AndroidResDependency.newInstance(
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
