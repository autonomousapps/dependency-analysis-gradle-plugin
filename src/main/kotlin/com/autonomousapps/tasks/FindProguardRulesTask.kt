@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.ProguardClasses
import com.autonomousapps.internal.utils.JAVA_FQCN_REGEX
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapNotNullToOrderedSet
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.io.File

/**
 * Find consumer proguard files in Android libraries. Assume any class reference found therein is
 * for a "used" class -- don't recommend removing a library that supplies such a class.
 */
@CacheableTask
abstract class FindProguardRulesTask : DefaultTask() {

  //TYPE_UNFILTERED_PROGUARD_RULES = "android-consumer-proguard-rules";
  //TYPE_FILTERED_PROGUARD_RULES = "android-filtered-proguard-rules";
  //TYPE_AAPT_PROGUARD_RULES = "android-aapt-proguard-rules";

  init {
    group = TASK_GROUP_DEP
    description = "Produces a report of all supported annotation types and their annotation processors"
  }

  private lateinit var proguardArtifacts: ArtifactCollection

  fun setProguardArtifacts(proguardArtifacts: ArtifactCollection) {
    this.proguardArtifacts = proguardArtifacts
  }

  // TODO double-check path-sensitivity
  @PathSensitive(PathSensitivity.ABSOLUTE)
  @InputFiles
  fun getProguardFiles(): FileCollection = proguardArtifacts.artifactFiles

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()

    val proguardClasses = proguardArtifacts
      .filter { it.file.isFile }
      .mapNotNullToOrderedSet { proguardRules ->
        try {
          val classes = extractClassReferencesFromProguardFile(proguardRules.file)
          ProguardClasses(classes, proguardRules.id.componentIdentifier)
        } catch (_: GradleException) {
          null
        }
      }

//    proguardClasses.forEach { println(it) }
    outputFile.writeText(proguardClasses.toJson())
  }

  /**
   * Uses a simple regex to parse Java class references from proguard rules files.
   */
  private fun extractClassReferencesFromProguardFile(file: File): Set<String> {
    // Find the class reference!
    // # This is necessary for default initialization using Camera2Config
    // -keep public class androidx.camera.camera2.Camera2Config$DefaultProvider { *; }
    val text = file.readText()
    return JAVA_FQCN_REGEX.findAll(text).map { it.value }
      .filterNot { it.startsWith("www") }
      .toSortedSet()
  }
}
