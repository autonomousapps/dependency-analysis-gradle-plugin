@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.Manifest
import com.autonomousapps.internal.utils.buildDocument
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapNotNullToOrderedSet
import com.autonomousapps.internal.utils.toJson
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

    val manifests: Set<Manifest> = manifestArtifacts.mapNotNullToOrderedSet { manifest ->
      try {
        extractPackageDeclarationFromManifest(manifest.file).let { (pn, hasComponent) ->
          Manifest(
            packageName = pn,
            hasComponents = hasComponent,
            componentIdentifier = manifest.id.componentIdentifier
          )
        }
      } catch (_: GradleException) {
        null
      }
    }

    outputFile.writeText(manifests.toJson())
  }

  private fun extractPackageDeclarationFromManifest(manifest: File): Pair<String, Boolean> {
    val document = buildDocument(manifest)
    return packageName(document) to hasAndroidComponent(document)
  }

  private fun packageName(document: Document): String {
    return document.getElementsByTagName("manifest").item(0)
      .attributes
      .getNamedItem("package")
      .nodeValue
  }

  // "service" is enough to catch LeakCanary, and "provider" makes sense in principle. Trying not to be too aggressive.
  private fun hasAndroidComponent(document: Document): Boolean {
//    if (document.getElementsByTagName("activity").length > 0) return true
    if (document.getElementsByTagName("service").length > 0) return true
//    if (document.getElementsByTagName("receiver").length > 0) return true
    if (document.getElementsByTagName("provider").length > 0) return true

    return false
  }
}
