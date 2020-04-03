@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.Manifest
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@CacheableTask
abstract class ManifestPackageExtractionTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces a report of packages, from other components, that are included via Android manifests"
  }

  private lateinit var manifestArtifacts: ArtifactCollection

  fun setArtifacts(manifestArtifacts: ArtifactCollection) {
    this.manifestArtifacts = manifestArtifacts
  }

  // Unfortunately the paths are absolute. They look like
  // ~/.gradle/caches/transforms-2/files-2.1/68044d2f962d3a8fe82e49e8213ba770/jetified-leakcanary-android-2.2/AndroidManifest.xml
  @PathSensitive(PathSensitivity.ABSOLUTE)
  @InputFiles
  fun getManifestFiles(): FileCollection = manifestArtifacts.artifactFiles

  @get:OutputFile
  abstract val manifestPackagesReport: RegularFileProperty

  @TaskAction fun action() {
    // Output
    val outputFile = manifestPackagesReport.get().asFile
    outputFile.delete()

    val manifests: Set<Manifest> = manifestArtifacts.mapNotNull { manifest ->
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
    }.toSortedSet()

    outputFile.writeText(manifests.toJson())
  }

  private fun extractPackageDeclarationFromManifest(manifest: File): Pair<String, Boolean> {
    val document = DocumentBuilderFactory.newInstance()
      .newDocumentBuilder()
      .parse(manifest)
    document.documentElement.normalize()

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
