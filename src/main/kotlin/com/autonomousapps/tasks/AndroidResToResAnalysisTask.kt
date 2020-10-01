package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.AndroidPublicRes
import com.autonomousapps.internal.utils.buildDocument
import com.autonomousapps.internal.utils.mapNotNull
import com.autonomousapps.internal.utils.mapNotNullToOrderedSet
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.io.File

/**
 * This task takes two inputs:
 * 1. Android res files declared by this project (xml)
 * 2. artifacts of type "android-public-res" (public.txt)
 *
 * We can parse the first for elements that might be present in the second. For example, if we have
 * ```
 * <resources>
 *   <style name="AppTheme" parent="Theme.AppCompat.Light.DarkActionBar">
 * </resources>
 * ```
 * we can expect to find, in public.txt, this line, associated with the dependency that supplies it (in this case
 * `'androidx.appcompat:appcompat'`):
 * ```
 * style Theme_AppCompat_Light_DarkActionBar
 * ```
 */
@CacheableTask
abstract class AndroidResToResToResAnalysisTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of all resources used by resources"
  }

  private lateinit var androidPublicRes: ArtifactCollection

  fun setAndroidPublicRes(androidPublicRes: ArtifactCollection) {
    this.androidPublicRes = androidPublicRes
  }

  /**
   * Artifact type "android-public-res".
   */
  @PathSensitive(PathSensitivity.NAME_ONLY)
  @InputFiles
  fun getAndroidPublicRes(): FileCollection = androidPublicRes.artifactFiles

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val androidLocalRes: ConfigurableFileCollection

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.get().asFile
    outputFile.delete()

    // Consumer (local usages)
    val candidates = androidLocalRes.flatMap {
      extractStyleParentsFromResourceXml(it)
    }.map {
      // Transform Theme.AppCompat.Light.DarkActionBar to Theme_AppCompat_Light_DarkActionBar
      it.replace(".", "_")
    }

    // Producer (dependencies)
    val usedDependencies: Set<AndroidPublicRes> = androidPublicRes.artifacts.mapNotNullToOrderedSet { res ->
      try {
        val lines = extractUsedLinesFromPublicRes(res.file, candidates)
        if (lines.isNotEmpty()) {
          AndroidPublicRes(
            lines = extractUsedLinesFromPublicRes(res.file, candidates),
            componentIdentifier = res.id.componentIdentifier
          )
        } else {
          null
        }
      } catch (_: GradleException) {
        null
      }
    }

    outputFile.writeText(usedDependencies.toJson())
  }

  private fun extractStyleParentsFromResourceXml(res: File): List<String> {
    val document = buildDocument(res)

    // e.g., "Theme.AppCompat.Light.DarkActionBar"
    return document.getElementsByTagName("style").mapNotNull {
      it.attributes.getNamedItem("parent")?.nodeValue
    }
  }

  private fun extractUsedLinesFromPublicRes(res: File, candidates: List<String>): List<AndroidPublicRes.Line> {
    return res.useLines { sequence ->
      sequence.filter { line ->
        candidates.any { candidate -> line.endsWith(candidate) }
      }.map {
        val split = it.split(" ")
        if (split.size != 2) {
          throw IllegalStateException("Lines in a public.txt file must have two items delimited by a single space")
        }
        AndroidPublicRes.Line(split[0], split[1])
      }.toList()
    }
  }
}
