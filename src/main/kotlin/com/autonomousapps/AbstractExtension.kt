package com.autonomousapps

import com.autonomousapps.extension.IssueHandler
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractExtension(private val objects: ObjectFactory) {

  internal abstract val issueHandler: IssueHandler

  private val adviceOutput = objects.fileProperty()

  internal var postProcessingTask: TaskProvider<out AbstractPostProcessingTask>? = null

  internal fun storeAdviceOutput(provider: Provider<RegularFile>) {
    val output = objects.fileProperty().also {
      it.set(provider)
    }
    adviceOutput.set(output)
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
   * Register your custom task that post-processes the [ProjectAdvice][com.autonomousapps.model.ProjectAdvice] produced
   * by this project.
   */
  @Suppress("unused") // explicit API
  fun registerPostProcessingTask(task: TaskProvider<out AbstractPostProcessingTask>) {
    postProcessingTask = task
    task.configure {
      input.set(adviceOutput())
    }
  }
}
