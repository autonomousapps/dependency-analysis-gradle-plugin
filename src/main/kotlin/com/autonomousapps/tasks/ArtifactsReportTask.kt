// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.Artifact
import com.autonomousapps.internal.ArtifactsExpander
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.filterNotOpaque
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.internal.ExcludedIdentifier
import com.autonomousapps.model.internal.PhysicalArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
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

  /** Needed to make sure task gives the same result if the build configuration in a composite changed between runs. */
  @get:Input
  public abstract val buildPath: Property<String>

  /**
   * Required for caching correctness. Without this, then
   * `ClassifiersSpec.transitive classifier dependencies do not lead to wrong advice` fails when the build cache is
   * enabled.
   */
  @get:Input
  public abstract val resolvedComponentResult: Property<ResolvedComponentResult>

  @get:Input
  public abstract val excludedIdentifiers: SetProperty<String>

  @get:Input
  public abstract val jarIds: ListProperty<ComponentArtifactIdentifier>

  @get:Input
  public abstract val jarVariants: ListProperty<ResolvedVariantResult>

  /**
   * This needs to use [InputFiles] and [PathSensitivity.ABSOLUTE] because the path to the jars really does matter here.
   * Using [Classpath] is an error, as it looks only at content and not name or path, and we really do need to know the
   * actual path to the artifact, even if its contents haven't changed.
   *
   * Attempts to make this path non-absolute have thus far failed. Please stop trying.
   */
  @get:PathSensitive(PathSensitivity.ABSOLUTE)
  @get:InputFiles
  public abstract val jarFiles: ListProperty<File>

  public fun withJarArtifacts(artifacts: Provider<Set<ResolvedArtifactResult>>) {
    jarIds.set(Artifact.ids(artifacts))
    jarVariants.set(Artifact.variants(artifacts))
    jarFiles.set(Artifact.files(artifacts))
  }

  @get:Input
  public abstract val opaqueJarIds: ListProperty<ComponentArtifactIdentifier>

  @get:Input
  public abstract val opaqueJarVariants: ListProperty<ResolvedVariantResult>

  /**
   * This needs to use [InputFiles] and [PathSensitivity.ABSOLUTE] because the path to the jars really does matter here.
   * Using [Classpath] is an error, as it looks only at content and not name or path, and we really do need to know the
   * actual path to the artifact, even if its contents haven't changed.
   *
   * Attempts to make this path non-absolute have thus far failed. Please stop trying.
   */
  @get:PathSensitive(PathSensitivity.ABSOLUTE)
  @get:InputFiles
  public abstract val opaqueJarFiles: ListProperty<File>

  public fun withOpaqueJarArtifacts(artifacts: Provider<Set<ResolvedArtifactResult>>) {
    opaqueJarIds.set(Artifact.ids(artifacts))
    opaqueJarVariants.set(Artifact.variants(artifacts))
    opaqueJarFiles.set(Artifact.files(artifacts))
  }

  /** [PhysicalArtifact]s used to compile or run main source. */
  @get:OutputFile
  public abstract val output: RegularFileProperty

  @get:OutputFile
  public abstract val excludedIdentifiersOutput: RegularFileProperty

  @TaskAction
  public fun action() {
    val output = output.getAndDelete()
    val excludedIdentifiersOutput = excludedIdentifiersOutput.getAndDelete()

    val allArtifacts = toPhysicalArtifacts(
      Artifact.sequenced(jarIds, jarVariants, jarFiles)
    )
    val opaqueArtifacts = toPhysicalArtifacts(
      Artifact.sequenced(opaqueJarIds, opaqueJarVariants, opaqueJarFiles)
    )
    val excludedIdentifiers = getExcludedIdentifiers()

    output.bufferWriteJsonSet(allArtifacts + opaqueArtifacts)
    excludedIdentifiersOutput.bufferWriteJsonSet(excludedIdentifiers)
  }

  private fun toPhysicalArtifacts(artifacts: Sequence<Artifact>): Set<PhysicalArtifact> {
    return artifacts
      .filterNotOpaque()
      .mapNotNull {
        try {
          val files = ArtifactsExpander.maybeExpand(it.file)
          PhysicalArtifact.of(
            artifact = it,
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
