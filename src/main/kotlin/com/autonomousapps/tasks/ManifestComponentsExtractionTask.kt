// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.ArtifactDetails
import com.autonomousapps.internal.ManifestParser
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.internal.AndroidManifestCapability.Component
import com.autonomousapps.model.internal.intermediates.producer.AndroidManifestDependency
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
public abstract class ManifestComponentsExtractionTask : DefaultTask() {

  init {
    description = "Produces a report of packages, from other components, that are included via Android manifests"
  }

  @get:Nested
  public abstract val manifestDetails: ListProperty<ArtifactDetails>

  @get:PathSensitive(PathSensitivity.NAME_ONLY)
  @get:InputFiles
  public abstract val manifestFiles: ListProperty<File>

  internal fun withManifests(artifacts: Provider<Set<ResolvedArtifactResult>>) {
    manifestDetails.set(ArtifactDetails.of(artifacts))
    manifestFiles.set(ArtifactDetails.files(artifacts))
  }

  @get:Input
  public abstract val namespace: Property<String>

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    val outputFile = output.getAndDelete()

    val parser = ManifestParser(namespace.get())

    val manifests: Set<AndroidManifestDependency> = ArtifactDetails.sequenced(manifestDetails, manifestFiles)
      .mapNotNull { (details, file) ->
        try {
          val parseResult = parser.parse(file, true)
          AndroidManifestDependency.newInstance(
            componentMap = parseResult.components.toComponentMap(),
            artifact = details,
          )
        } catch (_: GradleException) {
          null
        }
      }
      .toSortedSet()

    outputFile.bufferWriteJsonSet(manifests)
  }

  private fun Map<String, Set<String>>.toComponentMap(): Map<Component, Set<String>> {
    return map { (key, values) ->
      Component.of(key) to values
    }.toMap()
  }
}

