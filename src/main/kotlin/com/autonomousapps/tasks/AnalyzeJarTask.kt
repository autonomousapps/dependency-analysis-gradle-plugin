@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.AndroidLinterDependency
import com.autonomousapps.internal.Artifact
import com.autonomousapps.internal.Component
import com.autonomousapps.internal.JarAnalyzer
import com.autonomousapps.internal.utils.*
import com.autonomousapps.services.InMemoryCache
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * This task generates a report of all dependencies, whether or not they're transitive, and the classes they contain.
 */
@CacheableTask
abstract class AnalyzeJarTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of all direct and transitive dependencies"
  }

  /**
   * A [`Set<Artifact>`][Artifact].
   */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val buildArtifacts: RegularFileProperty

  @get:Optional
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val androidLinters: RegularFileProperty

  /**
   * A [`Set<Component>`][Component].
   */
  @get:OutputFile
  abstract val allComponentsReport: RegularFileProperty

  /**
   * A [`Set<Component>`][Component], pretty-printed.
   */
  @get:OutputFile
  abstract val allComponentsReportPretty: RegularFileProperty

  @get:Internal
  abstract val inMemoryCache: Property<InMemoryCache>

  @TaskAction
  fun action() {
    val outputFile = allComponentsReport.getAndDelete()
    val outputPrettyFile = allComponentsReportPretty.getAndDelete()

    val buildArtifacts = buildArtifacts.fromJsonList<Artifact>()
    val androidLinters = androidLinters.fromNullableJsonSet<AndroidLinterDependency>().orEmpty()

    // Actual work
    val components = JarAnalyzer(
      artifacts = buildArtifacts,
      androidLinters = androidLinters,
      logger = logger,
      inMemoryCache = inMemoryCache.get()
    ).components()

    // Write output to disk
    outputFile.writeText(components.toJson())
    outputPrettyFile.writeText(components.toPrettyString())
  }
}
