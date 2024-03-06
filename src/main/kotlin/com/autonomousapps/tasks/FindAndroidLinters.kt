// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.LINT_ISSUE_REGISTRY_PATH
import com.autonomousapps.internal.MANIFEST_PATH
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.intermediates.AndroidLinterDependency
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.BufferedReader
import java.io.File
import java.util.zip.ZipFile

/**
 * Produces a report of all android-lint jars on the compile classpath. An android-lint jar is a jar that contains
 * either a "Lint-Registry" listed in the jar's manifest, or an issue registry in the file [LINT_ISSUE_REGISTRY_PATH].
 */
@CacheableTask
abstract class FindAndroidLinters : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of dependencies that supply Android linters"
  }

  private lateinit var lintJars: ArtifactCollection

  fun setLintJars(lintJars: ArtifactCollection) {
    this.lintJars = lintJars
  }

  @Classpath
  fun getLintArtifactFiles(): FileCollection = lintJars.artifactFiles

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()

    val linters: Set<AndroidLinterDependency> = lintJars.asSequence()
      // Sometimes the file doesn't exist. Is this a bug? A feature? Who knows?
      .filter { it.file.exists() }
      .mapNotNull {
        try {
          AndroidLinterDependency(
            coordinates = it.toCoordinates(),
            lintRegistry = findLintRegistry(it.file)
          )
        } catch (e: GradleException) {
          null
        }
      }
      .toSortedSet()

    outputFile.bufferWriteJsonSet(linters)
  }

  private fun findLintRegistry(jar: File): String {
    val zip = ZipFile(jar)

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
