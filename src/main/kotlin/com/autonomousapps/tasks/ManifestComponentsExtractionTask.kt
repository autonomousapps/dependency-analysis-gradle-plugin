// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.internal.ManifestParser
import com.autonomousapps.internal.utils.bufferWriteJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapNotNullToOrderedSet
import com.autonomousapps.model.internal.AndroidManifestCapability.Component
import com.autonomousapps.model.internal.intermediates.producer.AndroidManifestDependency
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
public abstract class ManifestComponentsExtractionTask : DefaultTask() {

  init {
    description = "Produces a report of packages, from other components, that are included via Android manifests"
  }

  private lateinit var manifestArtifacts: ArtifactCollection

  public fun setArtifacts(manifestArtifacts: ArtifactCollection) {
    this.manifestArtifacts = manifestArtifacts
  }

  @PathSensitive(PathSensitivity.NAME_ONLY)
  @InputFiles
  public fun getManifestFiles(): FileCollection = manifestArtifacts.artifactFiles

  @get:Input
  public abstract val namespace: Property<String>

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    val outputFile = output.getAndDelete()

    val parser = ManifestParser(namespace.get())

    val manifests: Set<AndroidManifestDependency> = manifestArtifacts.mapNotNullToOrderedSet { manifest ->
      try {
        val parseResult = parser.parse(manifest.file, true)
        AndroidManifestDependency.newInstance(
          componentMap = parseResult.components.toComponentMap(),
          artifact = manifest,
        )
      } catch (_: GradleException) {
        null
      }
    }

    outputFile.bufferWriteJsonSet(manifests)
  }

  private fun Map<String, Set<String>>.toComponentMap(): Map<Component, Set<String>> {
    return map { (key, values) ->
      Component.of(key) to values
    }.toMap()
  }
}

