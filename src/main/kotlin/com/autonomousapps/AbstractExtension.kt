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
abstract class AbstractExtension(private val objects: ObjectFactory) {

  private val logger = getLogger<DependencyAnalysisPlugin>()

  internal abstract val issueHandler: IssueHandler

  private val adviceOutput = objects.fileProperty()
  private val abiDumpOutputs = mutableMapOf<String, RegularFileProperty>()

  internal var postProcessingTask: TaskProvider<out AbstractPostProcessingTask>? = null

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
   * Returns the output from the project-level advice.
   *
   * Never null, but may _contain_ a null value. Use with [RegularFileProperty.getOrNull].
   */
  fun adviceOutput(): RegularFileProperty {
    return adviceOutput
  }

  /**
   * Register your custom task that post-processes the [ComprehensiveAdvice][com.autonomousapps.advice.ComprehensiveAdvice]
   * (v1) or [ProjectAdvice][com.autonomousapps.model.ProjectAdvice] (v2) produced by this project.
   */
  fun registerPostProcessingTask(task: TaskProvider<out AbstractPostProcessingTask>) {
    postProcessingTask = task
    task.configure {
      input.set(adviceOutput())
    }
  }
}
