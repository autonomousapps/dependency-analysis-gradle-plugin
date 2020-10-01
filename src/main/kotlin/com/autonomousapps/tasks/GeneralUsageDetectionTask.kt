@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.Component
import com.autonomousapps.internal.Imports
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * This detects _any_ usage, based on presence of imports that can be associated with dependencies.
 * It is sort of a catch-all / edge-case detector.
 */
abstract class GeneralUsageDetectionTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of dependencies that are used based on the heuristic that " +
      "import statements appear in project source"
  }

  /**
   * Class usages, as `List<Component>`.
   */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val components: RegularFileProperty

  /**
   * All the imports in the Java and Kotlin source in this project.
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val imports: RegularFileProperty

  /**
   * A [`Set<Dependency>`][Dependency] of dependencies that provide types that the current project
   * is using.
   */
  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(GeneralUsageDetectionWorkAction::class.java) {
      components.set(this@GeneralUsageDetectionTask.components)
      importsFile.set(this@GeneralUsageDetectionTask.imports)
      output.set(this@GeneralUsageDetectionTask.output)
    }
  }
}

interface GeneralUsageDetectionParameters : WorkParameters {
  val components: RegularFileProperty
  val importsFile: RegularFileProperty
  val output: RegularFileProperty
}

abstract class GeneralUsageDetectionWorkAction : WorkAction<GeneralUsageDetectionParameters> {

  private val logger = getLogger<ConstantUsageDetectionTask>()

  override fun execute() {
    // Output
    val constantUsageReportFile = parameters.output.getAndDelete()

    // Inputs
    val components = parameters.components.get().asFile.readText().fromJsonList<Component>()
    val imports = parameters.importsFile.get().asFile.readText().fromJsonList<Imports>().flatten()

    val usedDependencies = findUsedDependencies(components, imports)

    logger.debug("Constants usage:\n${usedDependencies.toPrettyString()}")
    constantUsageReportFile.writeText(usedDependencies.toJson())
  }

  /**
   * Given components:
   * ```
   * [{"dependency":{"identifier":":proj-2","configurationName":"implementation"},"isTransitive":false,"isCompileOnlyAnnotations":false,"classes":["com.example.lib.Library"]}]
   * ```
   * and imports:
   * ```
   * ["com.example.lib.Library"]
   * ```
   * return `Dependency(':proj-2')`
   */
  private fun findUsedDependencies(
    components: List<Component>, imports: Set<String>
  ): Set<Dependency> {
    return imports.mapNotNull { import ->
      components.find { component ->
        component.classes.contains(import)
      }
    }.mapToSet { it.dependency }
  }

  // The detector doesn't care about source type
  private fun List<Imports>.flatten(): Set<String> = flatMapToOrderedSet { it.imports }
}
