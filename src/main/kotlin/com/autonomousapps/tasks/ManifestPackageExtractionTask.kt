@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.Manifest
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

    val manifests: Set<Manifest> = manifestArtifacts.mapNotNullToOrderedSet { manifest ->
      try {
        extractManifestComponents(manifest.file).let { (pn, componentMap) ->
          Manifest(
            packageName = pn,
            componentMap = componentMap,
            componentIdentifier = manifest.id.componentIdentifier
          )
        }
      } catch (_: GradleException) {
        null
      }
    }

    outputFile.writeText(manifests.toJson())
  }

  // "service" is enough to catch LeakCanary, and "provider" makes sense in principle. Trying not to be too aggressive.
  private fun extractManifestComponents(manifest: File): Pair<String, Map<String, Set<String>>> {
    val document = buildDocument(manifest)

    val pn = packageName(document)
//    val activities = document.componentNames(Component.ACTIVITY, pn)
//    val receivers = document.componentNames(Component.RECEIVER, pn)
    val services = document.componentNames(Manifest.Component.SERVICE, pn)
    val providers = document.componentNames(Manifest.Component.PROVIDER, pn)

    val map = mutableMapOf<String, Set<String>>()
//    if (activities.isNotEmpty()) map[Manifest.Component.ACTIVITY.plural] = activities
//    if (receivers.isNotEmpty()) map[Manifest.Component.RECEIVER.plural] = receivers
    if (services.isNotEmpty()) map[Manifest.Component.SERVICE.mapKey] = services
    if (providers.isNotEmpty()) map[Manifest.Component.PROVIDER.mapKey] = providers

    return packageName(document) to map
  }

  private fun packageName(document: Document): String {
    return document.getElementsByTagName("manifest").item(0)
      .attributes
      .getNamedItem("package")
      .nodeValue
  }

  private fun Document.componentNames(component: Manifest.Component, packageName: String): Set<String> {
    return getElementsByTagName(component.tagName)
      .mapToSet { it.attributes.getNamedItem(component.attrName).nodeValue.withPackageName(packageName) }
  }

  private fun String.withPackageName(packageName: String): String {
    return if (startsWith(".")) {
      // item name is relative, so prefix with the package name
      "$packageName$this"
    } else {
      // item name is absolute, so use it as-is
      this
    }
  }
}
