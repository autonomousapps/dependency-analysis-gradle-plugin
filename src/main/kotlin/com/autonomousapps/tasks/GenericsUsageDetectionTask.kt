@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.Component
import com.autonomousapps.internal.Imports
import com.autonomousapps.internal.utils.*
import com.autonomousapps.internal.utils.getLogger
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * TODO in fact this task looks at all imports, not just those that provide generics. Finding JUST
 * generics would require elaborating on the antlr grammar, which will take a while.
 */
abstract class GenericsUsageDetectionTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces a report of types, from other components, that have been used in a generic context"
  }

  /**
   * Upstream artifacts.
   */
//  @get:PathSensitive(PathSensitivity.RELATIVE)
//  @get:InputFile
//  abstract val artifacts: RegularFileProperty

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
   * A [`Set<Dependency>`][Dependency] of dependencies that provide types that the current project is using.
   */
  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(GenericsUsageDetectionWorkAction::class.java) {
      components.set(this@GenericsUsageDetectionTask.components)
      importsFile.set(this@GenericsUsageDetectionTask.imports)
      output.set(this@GenericsUsageDetectionTask.output)
    }
  }
}

interface GenericsUsageDetectionParameters : WorkParameters {
  val components: RegularFileProperty
  val importsFile: RegularFileProperty
  val output: RegularFileProperty
}

abstract class GenericsUsageDetectionWorkAction : WorkAction<GenericsUsageDetectionParameters> {

  private val logger = getLogger<ConstantUsageDetectionTask>()

  override fun execute() {
    // Output
    val constantUsageReportFile = parameters.output.getAndDelete()

    // Inputs
    val components = parameters.components.get().asFile.readText().fromJsonList<Component>()
    val imports = parameters.importsFile.get().asFile.readText().fromJsonList<Imports>().flatten()

    val usedDependencies = findUsedDependencies(components, imports)//JvmConstantDetector(logger, artifacts, imports).find()

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


//    val usedDependencies = mutableSetOf<Dependency>()
//    imports.forEach { import ->
//      components.find { component ->
//        component.classes.contains(import)
//      }?.let {
//        usedDependencies.add(it.dependency)
//      }
//    }
//    return usedDependencies
  }

  // The detector doesn't care about source type
  private fun List<Imports>.flatten(): Set<String> = flatMapToOrderedSet { it.imports }
}
