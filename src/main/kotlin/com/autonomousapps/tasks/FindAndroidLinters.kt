// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.ArtifactDetails
import com.autonomousapps.internal.LINT_ISSUE_REGISTRY_PATH
import com.autonomousapps.internal.MANIFEST_PATH
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.internal.intermediates.producer.AndroidLinterDependency
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.BufferedReader
import java.io.File
import java.util.zip.ZipFile

/**
 * Produces a report of all android-lint jars on the compile classpath. An android-lint jar is a jar that contains
 * either a "Lint-Registry" listed in the jar's manifest, or an issue registry in the file [LINT_ISSUE_REGISTRY_PATH].
 */
@CacheableTask
public abstract class FindAndroidLinters : DefaultTask() {

  init {
    description = "Produces a report of dependencies that supply Android linters"
  }

  @get:Nested
  public abstract val lintDetails: ListProperty<ArtifactDetails>

  @get:Classpath
  public abstract val lintFiles: ListProperty<File>

  internal fun withLintJars(artifacts: Provider<Set<ResolvedArtifactResult>>) {
    lintDetails.set(ArtifactDetails.of(artifacts))
    lintFiles.set(ArtifactDetails.files(artifacts))
  }

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    val outputFile = output.getAndDelete()

    val linters: Set<AndroidLinterDependency> = ArtifactDetails.sequenced(lintDetails, lintFiles)
      // Sometimes the file doesn't exist. Is this a bug? A feature? Who knows?
      .filter { (_, file) -> file.exists() }
      .mapNotNull { (details, file) ->
        try {
          AndroidLinterDependency(
            coordinates = details.toCoordinates(),
            lintRegistry = findLintRegistry(file)
          )
        } catch (_: GradleException) {
          null
        }
      }
      .toSortedSet()

    outputFile.bufferWriteJsonSet(linters)
  }

  private fun findLintRegistry(jar: File): String {
    ZipFile(jar).use { zip ->
      val manifestEntry: String? = zip.getEntry(MANIFEST_PATH)?.run {
        zip.getInputStream(this).bufferedReader().use(BufferedReader::readLines)
          .find { it.startsWith("Lint-Registry") }
          ?.substringAfter(":")
          ?.trim()
      }
      if (manifestEntry != null) return manifestEntry

      val serviceEntry: String? = zip.getEntry(LINT_ISSUE_REGISTRY_PATH)?.run {
        zip.getInputStream(this).bufferedReader().use(BufferedReader::readLines)
          .first()
          .trim()
      }
      if (serviceEntry != null) return serviceEntry

      // One of the above should be non-null
      throw GradleException("No linter issue registry for ${jar.path}")
    }
  }
}
