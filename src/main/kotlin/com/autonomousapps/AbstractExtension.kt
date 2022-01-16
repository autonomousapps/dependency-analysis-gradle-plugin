@file:Suppress("UnstableApiUsage", "unused")

package com.autonomousapps

import com.autonomousapps.extension.IssueHandler
import com.autonomousapps.internal.utils.getLogger
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractExtension(
  private val objects: ObjectFactory,
  protected val isV1: Boolean
) {

  private val logger = getLogger<DependencyAnalysisPlugin>()

  internal abstract val issueHandler: IssueHandler

  private val adviceOutput = objects.fileProperty()
  private val abiDumpOutputs = mutableMapOf<String, RegularFileProperty>()

  internal var postProcessingTask: TaskProvider<out AbstractPostProcessingTask>? = null
  private val abiPostProcessingTasks = mutableMapOf<String, TaskProvider<out AbstractAbiPostProcessingTask>>()

  internal fun storeAdviceOutput(provider: Provider<RegularFile>) {
    val output = objects.fileProperty().also {
      it.set(provider)
    }
    adviceOutput.set(output)
  }

  internal fun storeAbiDumpOutput(provider: Provider<RegularFile>, variantName: String) {
    val output = objects.fileProperty().also {
      it.set(provider)
    }
    abiDumpOutputs.putIfAbsent(variantName, output)?.also {
      logger.warn("Attempt to add output to $variantName ignored")
    }
  }

  /**
   * Returns the output from the project-level holistic advice, produced by the
   * [AdviceSubprojectAggregationTask][com.autonomousapps.tasks.AdviceSubprojectAggregationTask].
   * This output is a [com.autonomousapps.advice.ComprehensiveAdvice]
   *
   * Never null, but may _contain_ a null value. Use with [RegularFileProperty.getOrNull].
   */
  fun adviceOutput(): RegularFileProperty {
    return adviceOutput
  }

  /**
   * Returns the output from the variant-level ABI dump task, produced by the
   * [AbiAnalysisTask][com.autonomousapps.tasks.AbiAnalysisTask].
   * This output is a simple text file.
   *
   * Never null, but may _contain_ a null value. Use with [RegularFileProperty.getOrNull].
   */
  fun abiDumpOutputFor(variantName: String): RegularFileProperty {
    return abiDumpOutputs[variantName] ?: error("Missing ABI dump output for $variantName")
  }

  /**
   * Register your custom task that post-processes the [com.autonomousapps.advice.ComprehensiveAdvice]
   * produced by this project.
   */
  fun registerPostProcessingTask(task: TaskProvider<out AbstractPostProcessingTask>) {
    postProcessingTask = task
    task.configure {
      version1.set(isV1)
      input.set(adviceOutput())
    }
  }

  fun registerAbiPostProcessingTask(
    task: TaskProvider<out AbstractAbiPostProcessingTask>, variantName: String
  ) {
    abiPostProcessingTasks.putIfAbsent(variantName, task)?.also {
      logger.warn("An ABI post-processing task has already been registered for variant $variantName")
    }
    task.configure {
      input.set(abiDumpOutputFor(variantName))
    }
  }

  fun abiPostProcessingTaskFor(variantName: String): TaskProvider<out AbstractAbiPostProcessingTask>? {
    return abiPostProcessingTasks[variantName]
  }
}
