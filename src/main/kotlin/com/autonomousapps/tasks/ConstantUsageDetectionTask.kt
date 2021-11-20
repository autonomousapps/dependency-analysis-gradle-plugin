@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.Component
import com.autonomousapps.internal.Imports
import com.autonomousapps.internal.JvmConstantDetector
import com.autonomousapps.internal.utils.*
import com.autonomousapps.services.InMemoryCache
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
abstract class ConstantUsageDetectionTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of constants, from other components, that have been used"
  }

  /**
   * Upstream components.
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val components: RegularFileProperty

  /**
   * All the imports in the Java and Kotlin source in this project.
   */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val imports: RegularFileProperty

  /**
   * A [`Set<Dependency>`][Dependency] of dependencies that provide constants that the current project is using.
   */
  @get:OutputFile
  abstract val constantUsageReport: RegularFileProperty

  @get:Internal
  abstract val inMemoryCacheProvider: Property<InMemoryCache>

  @TaskAction
  fun action() {
    workerExecutor.noIsolation().submit(ConstantUsageDetectionWorkAction::class.java) {
      components.set(this@ConstantUsageDetectionTask.components)
      importsFile.set(this@ConstantUsageDetectionTask.imports)
      constantUsageReport.set(this@ConstantUsageDetectionTask.constantUsageReport)
      inMemoryCacheProvider.set(this@ConstantUsageDetectionTask.inMemoryCacheProvider)
    }
  }
}

interface ConstantUsageDetectionParameters : WorkParameters {
  val components: RegularFileProperty
  val importsFile: RegularFileProperty
  val constantUsageReport: RegularFileProperty
  val inMemoryCacheProvider: Property<InMemoryCache>
}

abstract class ConstantUsageDetectionWorkAction : WorkAction<ConstantUsageDetectionParameters> {

  private val logger = getLogger<ConstantUsageDetectionTask>()

  override fun execute() {
    // Output
    val constantUsageReportFile = parameters.constantUsageReport.getAndDelete()

    // Inputs
    val components = parameters.components.fromJsonList<Component>()
    val imports = parameters.importsFile.fromJsonList<Imports>().flatten()

    val usedComponents = JvmConstantDetector(parameters.inMemoryCacheProvider, components, imports).find()

    logger.debug("Constants usage:\n${usedComponents.toPrettyString()}")
    constantUsageReportFile.writeText(usedComponents.toJson())
  }

  // The constant detector doesn't care about source type
  private fun List<Imports>.flatten(): Set<String> = flatMapToOrderedSet {
    it.imports.flatMap { it.value }
  }
}
