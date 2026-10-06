// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.ArtifactDetails
import com.autonomousapps.internal.ArtifactsExpander
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.filterNonGradle
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.internal.ExcludedIdentifier
import com.autonomousapps.model.internal.PhysicalArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import java.io.File

/**
 * Produces a report of all the artifacts required to build the given project; i.e., the artifacts on the compile
 * classpath, the runtime classpath, and a few others. See [com.autonomousapps.model.internal.declaration.Locator] for
 * the full list of analyzed [Configuration][org.gradle.api.artifacts.Configuration]s. These artifacts are physical
 * files on disk, such as jars.
 */
@CacheableTask
public abstract class ArtifactsReportTask : DefaultTask() {

  init {
    description = "Produces a report that lists all direct and transitive dependencies, along with their artifacts"
  }

  @get:Nested
  public abstract val jarDetails: ListProperty<ArtifactDetails>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val jarFiles: ListProperty<File>

  /**
   * This artifact collection is the result of resolving the compile or runtime classpath for jar artifacts.
   *
   * This needs to be public because `ComponentWithMultipleArtifactsSpec.one component can have multiple Jars produced by a transform`
   * configures it.
   */
  public fun withJarArtifacts(artifacts: Provider<Set<ResolvedArtifactResult>>) {
    jarDetails.set(ArtifactDetails.of(artifacts))
    jarFiles.set(ArtifactDetails.files(artifacts))
  }

  @get:Nested
  public abstract val opaqueDetails: ListProperty<ArtifactDetails>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val opaqueFiles: ListProperty<File>

  /**
   * This artifact collection is the result of resolving the compile or runtime classpath for
   * [OpaqueComponentArtifactIdentifiers][org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier].
   */
  internal fun withOpaqueArtifacts(artifacts: Provider<Set<ResolvedArtifactResult>>) {
    opaqueDetails.set(ArtifactDetails.of(artifacts))
    opaqueFiles.set(ArtifactDetails.files(artifacts))
  }

  /** Needed to make sure task gives the same result if the build configuration in a composite changed between runs. */
  @get:Input
  public abstract val buildPath: Property<String>

  @get:Input
  public abstract val excludedIdentifiers: SetProperty<String>

  /** [PhysicalArtifact]s used to compile or run main source. */
  @get:OutputFile
  public abstract val output: RegularFileProperty

  @get:OutputFile
  public abstract val excludedIdentifiersOutput: RegularFileProperty

  @TaskAction
  public fun action() {
    val output = output.getAndDelete()
    val excludedIdentifiersOutput = excludedIdentifiersOutput.getAndDelete()

    val allArtifacts = toPhysicalArtifacts(ArtifactDetails.sequenced(jarDetails, jarFiles))
    val opaqueArtifacts = toPhysicalArtifacts(ArtifactDetails.sequenced(opaqueDetails, opaqueFiles))
    val excludedIdentifiers = getExcludedIdentifiers()

    output.bufferWriteJsonSet(allArtifacts + opaqueArtifacts)
    excludedIdentifiersOutput.bufferWriteJsonSet(excludedIdentifiers)
  }

  private fun toPhysicalArtifacts(artifacts: Sequence<Pair<ArtifactDetails, File>>): Set<PhysicalArtifact> {
    return artifacts
      .filterNonGradle()
      .mapNotNull { (details, file) ->
        try {
          val files = ArtifactsExpander.maybeExpand(file)
          PhysicalArtifact.of(
            artifact = details,
            files = files,
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
