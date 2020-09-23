@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.Artifact
import com.autonomousapps.internal.Component
import com.autonomousapps.internal.JarAnalyzer
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.internal.utils.toPrettyString
import com.autonomousapps.services.InMemoryCache
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * This task generates a report of all dependencies, whether or not they're transitive, and the
 * classes they contain. Currently uses `${variant}RuntimeClasspath`, which has visibility into all
 * dependencies, including transitive (and including those 'hidden' by `implementation`), as well as
 * `runtimeOnly`.
 * TODO this is perhaps wrong/unnecessary. See TODO below.
 */
@CacheableTask
abstract class FindClassesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces a report of all direct and transitive dependencies"
  }

  /**
   * This is the "official" input for wiring task dependencies correctly, but is otherwise unused.
   * It is the result of resolving `runtimeClasspath`. cf. [configuration]
   */
  @get:Classpath
  abstract val artifactFiles: ConfigurableFileCollection

  /**
   * This is what the task actually uses as its input. We really only care about the
   * [ResolutionResult]. cf. [artifactFiles].
   */
  @get:Internal
  lateinit var configuration: Configuration

  /**
   * A [`Set<Artifact>`][Artifact].
   */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val allArtifacts: RegularFileProperty

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
  abstract val inMemoryCacheProvider: Property<InMemoryCache>

  @TaskAction
  fun action() {
    // Outputs
    val outputFile = allComponentsReport.getAndDelete()
    val outputPrettyFile = allComponentsReportPretty.getAndDelete()

    // Inputs
    // This includes both direct and transitive dependencies, hence "all"
    val allArtifacts = allArtifacts.get().asFile.readText().fromJsonList<Artifact>()

    // Build services
    val inMemoryCache = inMemoryCacheProvider.get()

    // Actual work
    val components = JarAnalyzer(
      // TODO I suspect I don't need to use the runtimeClasspath for getting this set of "direct artifacts"
      configuration,
      allArtifacts,
      logger,
      inMemoryCache
    ).components()

    // Write output to disk
    outputFile.writeText(components.toJson())
    outputPrettyFile.writeText(components.toPrettyString())
  }
}
