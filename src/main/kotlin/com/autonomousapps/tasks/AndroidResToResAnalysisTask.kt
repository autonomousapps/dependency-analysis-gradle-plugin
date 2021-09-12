package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.AndroidPublicRes
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.w3c.dom.Document
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
   * Artifact type "android-public-res". Appears to only be for platform dependencies that bother to include a
   * `public.xml`.
   */
  @PathSensitive(PathSensitivity.NAME_ONLY)
  @InputFiles
  fun getAndroidPublicRes(): FileCollection = androidPublicRes.artifactFiles

  private lateinit var androidSymbols: ArtifactCollection

  fun setAndroidSymbols(androidSymbols: ArtifactCollection) {
    this.androidSymbols = androidSymbols
  }

  /**
   * Artifact type "android-symbol-with-package-name". All Android libraries seem to have this.
   */
  @PathSensitive(PathSensitivity.NAME_ONLY)
  @InputFiles
  fun getAndroidSymbols(): FileCollection = androidSymbols.artifactFiles

  /**
   * Consumer XML files.
   */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val androidLocalRes: ConfigurableFileCollection

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()

    // Consumer (local usages)
    val candidates = AndroidResParser(androidLocalRes)

    // Producer (dependencies)
    val usedDependencies = mutableSetOf<AndroidPublicRes>()

    fun extractLinesFromRes(artifactCollection: ArtifactCollection, candidates: Set<Res>) {
      usedDependencies += artifactCollection.artifacts.mapNotNullToOrderedSet { res ->
        try {
          val lines = extractUsedLinesFromRes(res.file, candidates)
          if (lines.isNotEmpty()) {
            AndroidPublicRes(
              lines = lines,
              componentIdentifier = res.id.componentIdentifier
            )
          } else {
            null
          }
        } catch (_: GradleException) {
          null
        }
      }
    }

    extractLinesFromRes(androidPublicRes, candidates.getStyleParentCandidates())
    extractLinesFromRes(androidSymbols, candidates.getAttrsCandidates())

    outputFile.writeText(usedDependencies.toJson())
  }


  private fun extractUsedLinesFromRes(
    res: File, candidates: Set<Res>
  ): List<AndroidPublicRes.Line> = res.useLines { strings ->
    strings
      .mapNotNull { line ->
        // TODO what if the format changes and it's no longer two items delimited by a space?
        val split = line.split(' ')
        if (split.size == 2) {
          split[0] to split[1]
        } else {
          null
        }
      }.filter { (type, value) ->
        candidates.any { candidate ->
          when (candidate) {
            is StyleParentRes -> value == candidate.styleParent
            is AttrRes -> type == "attr" && value == candidate.attr
          }
        }
      }.map { (type, value) ->
        AndroidPublicRes.Line(type, value)
      }.toList()
  }

  private class AndroidResParser(resources: Iterable<File>) {

    private val styleParentCandidates = mutableSetOf<StyleParentRes>()
    private val attrsCandidates = mutableSetOf<AttrRes>()

    fun getStyleParentCandidates() = styleParentCandidates.toSet()
    fun getAttrsCandidates() = attrsCandidates.toSet()

    init {
      val docs = resources.map { buildDocument(it) }
      docs.forEach {
        styleParentCandidates += extractStyleParentsFromResourceXml(it)
        attrsCandidates += extractAttrsFromResourceXml(it)
      }
    }

    // e.g., "Theme.AppCompat.Light.DarkActionBar"
    private fun extractStyleParentsFromResourceXml(doc: Document) =
      doc.getElementsByTagName("style").mapNotNull {
        it.attributes.getNamedItem("parent")?.nodeValue
      }.mapToSet {
        // Transform Theme.AppCompat.Light.DarkActionBar to Theme_AppCompat_Light_DarkActionBar
        it.replace('.', '_')
      }.mapToSet {
        StyleParentRes(it)
      }

    // e.g., "?themeColor"
    private fun extractAttrsFromResourceXml(doc: Document) =
      doc.attrs().values
        .filterToSet { it.startsWith("?") }
        .mapToSet { it.removePrefix("?") }
        .mapToSet { AttrRes(it) }
  }
}

/** Represents a resource usage on the consumer side. */
private sealed class Res

/** The parent of a style resource, e.g. "Theme.AppCompat.Light.DarkActionBar". */
private data class StyleParentRes(val styleParent: String) : Res()

/** A theme reference, such as "?themeColor". */
private data class AttrRes(val attr: String) : Res()
