@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.ManifestParser
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapNotNullToOrderedSet
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.AndroidManifestCapability.Component
import com.autonomousapps.model.intermediates.AndroidManifestDependency
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ManifestComponentsExtractionTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of packages, from other components, that are included via Android manifests"
  }

  private lateinit var manifestArtifacts: ArtifactCollection

  fun setArtifacts(manifestArtifacts: ArtifactCollection) {
    this.manifestArtifacts = manifestArtifacts
  }

  @PathSensitive(PathSensitivity.NAME_ONLY)
  @InputFiles
  fun getManifestFiles(): FileCollection = manifestArtifacts.artifactFiles

  @get:Input
  abstract val namespace: Property<String>

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()

    val parser = ManifestParser(namespace.get())

    val manifests: Set<AndroidManifestDependency> = manifestArtifacts.mapNotNullToOrderedSet { manifest ->
      try {
        val parseResult = parser.parse(manifest.file, true)
        AndroidManifestDependency(
          componentMap = parseResult.components.toComponentMap(),
          artifact = manifest
        )
      } catch (_: GradleException) {
        null
      }
    }

    outputFile.writeText(manifests.toJson())
  }

  private fun Map<String, Set<String>>.toComponentMap(): Map<Component, Set<String>> {
    return map { (key, values) ->
      Component.of(key) to values
    }.toMap()
  }
}

