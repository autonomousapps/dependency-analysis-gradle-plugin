@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.Manifest
import com.autonomousapps.internal.ManifestParser
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.w3c.dom.Document
import java.io.File

@CacheableTask
abstract class ManifestPackageExtractionTask : DefaultTask() {

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

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()

    val parser = ManifestParser()

    val manifests: Set<Manifest> = manifestArtifacts.mapNotNullToOrderedSet { manifest ->
      try {
        val parseResult = parser.parse(manifest.file)
        Manifest(
          packageName = parseResult.packageName,
          componentMap = parseResult.components,
          componentIdentifier = manifest.id.componentIdentifier
        )
      } catch (_: GradleException) {
        null
      }
    }

    outputFile.writeText(manifests.toJson())
  }
}
