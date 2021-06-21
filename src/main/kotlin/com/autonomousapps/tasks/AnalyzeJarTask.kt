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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * This task generates a report of all dependencies, whether or not they're transitive, and the
 * classes they contain.
 */
@CacheableTask
abstract class AnalyzeJarTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of all direct and transitive dependencies"
  }

  /**
   * This is the "official" input for wiring task dependencies correctly, but is otherwise unused.
   * It is the result of resolving `compileClasspath`. cf. [compileClasspath].
   */
  @get:Classpath
  abstract val artifactFiles: ConfigurableFileCollection

  /**
   * This is what the task actually uses as its input. We really only care about the
   * [ResolutionResult]. cf. [artifactFiles].
   */
  @get:Internal
  lateinit var compileClasspath: Configuration

  /**
   * This is the "official" input for wiring task dependencies correctly, but is otherwise unused.
   * It is the result of resolving `testCompileClasspath`. cf. [testCompileClasspath].
   *
   * May be absent if, e.g., Android unit tests are disabled for some variant.
   */
  @get:Optional
  @get:Classpath
  abstract val testArtifactFiles: ConfigurableFileCollection

  @get:Internal
  var testCompileClasspath: Configuration? = null

  /**
   * A [`Set<Artifact>`][Artifact].
   */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val allArtifacts: RegularFileProperty

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
  abstract val inMemoryCacheProvider: Property<InMemoryCache>

  @TaskAction
  fun action() {
    // Outputs
    val outputFile = allComponentsReport.getAndDelete()
    val outputPrettyFile = allComponentsReportPretty.getAndDelete()

    // Inputs
    // This includes both direct and transitive dependencies, hence "all"
    val allArtifacts = allArtifacts.fromJsonList<Artifact>()
    val androidLinters = androidLinters.fromNullableJsonSet<AndroidLinterDependency>().orEmpty()

    // Actual work
    val components = JarAnalyzer(
      compileClasspath = compileClasspath,
      testCompileClasspath = testCompileClasspath,
      artifacts = allArtifacts,
      androidLinters = androidLinters,
      logger = logger,
      inMemoryCache = inMemoryCacheProvider.get()
    ).components()

    // Write output to disk
    outputFile.writeText(components.toJson())
    outputPrettyFile.writeText(components.toPrettyString())
  }
}
