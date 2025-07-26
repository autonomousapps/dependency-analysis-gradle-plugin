// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.filterNonGradle
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.internal.ExcludedIdentifier
import com.autonomousapps.model.internal.PhysicalArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*

/**
 * Produces a report of all the artifacts required to build the given project; i.e., the artifacts on the compile
 * classpath, the runtime classpath, and a few others. See
 * [FindDeclarationsTask.Locator] for the full list of analyzed [Configuration][org.gradle.api.artifacts.Configuration]s. These artifacts are
 * physical files on disk, such as jars.
 */
@CacheableTask
public abstract class ArtifactsReportTask : DefaultTask() {

  init {
    description = "Produces a report that lists all direct and transitive dependencies, along with their artifacts"
  }

  @get:Internal
  public abstract val artifacts: Property<ArtifactCollection>

  /**
   * This is the "official" input for wiring task dependencies correctly, but is otherwise
   * unused. This needs to use [InputFiles] and [PathSensitivity.ABSOLUTE] because the path to the
   * jars really does matter here. Using [Classpath] is an error, as it looks only at content and
   * not name or path, and we really do need to know the actual path to the artifact, even if its
   * contents haven't changed.
   */
  @PathSensitive(PathSensitivity.ABSOLUTE)
  @InputFiles // TODO(tsr): can I avoid using `get()`?
  public fun getClasspathArtifactFiles(): FileCollection = artifacts.get().artifactFiles

  /**
   * This artifact collection is the result of resolving the compile or runtime classpath.
   */
  public fun setConfiguration(
    configuration: NamedDomainObjectProvider<Configuration>,
    action: (Configuration) -> ArtifactCollection,
  ) {
    excludedIdentifiers.set(configuration.map { c -> c.excludeRules.map { "${it.group}:${it.module}".intern() } })
    artifacts.set(configuration.map { c -> action(c) })
  }

  /** Needed to make sure task gives the same result if the build configuration in a composite changed between runs. */
  @get:Input
  public abstract val buildPath: Property<String>

  @get:Input
  public abstract val excludedIdentifiers: SetProperty<String>

  /**
   * [PhysicalArtifact]s used to compile or run main source.
   */
  @get:OutputFile
  public abstract val output: RegularFileProperty

  @get:OutputFile
  public abstract val excludedIdentifiersOutput: RegularFileProperty

  @TaskAction
  public fun action() {
    val output = output.getAndDelete()
    val excludedIdentifiersOutput = excludedIdentifiersOutput.getAndDelete()

    val allArtifacts = toPhysicalArtifacts(artifacts.get())//artifacts)
    val excludedIdentifiers = getExcludedIdentifiers()

    output.bufferWriteJsonSet(allArtifacts)
    excludedIdentifiersOutput.writeText(excludedIdentifiers.toJson())
  }

  private fun toPhysicalArtifacts(artifacts: ArtifactCollection): Set<PhysicalArtifact> {
    return artifacts.asSequence()
      .filterNonGradle()
      .mapNotNull {
        try {
          // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/948#issuecomment-1711177139
          val file = if (it.file.path.endsWith("kotlin/main") || it.file.path.endsWith("java/main")) {
            it.file.parentFile!!.parentFile!!
          } else {
            it.file
          }
          PhysicalArtifact.of(
            artifact = it,
            file = file
          )
        } catch (_: GradleException) {
          null
        }
      }
      .toSortedSet()
  }

  private fun getExcludedIdentifiers(): Set<ExcludedIdentifier> {
    return excludedIdentifiers.get().asSequence()
      .map { ExcludedIdentifier(it.intern()) }
      .toSortedSet()
  }
}
