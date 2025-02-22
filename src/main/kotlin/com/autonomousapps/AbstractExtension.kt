// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.autonomousapps.extension.*
import com.autonomousapps.services.GlobalDslService
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

abstract class AbstractExtension @Inject constructor(
  private val objects: ObjectFactory,
  gradle: Gradle,
) {

  internal companion object {
    const val NAME = "dependencyAnalysis"
  }

  private val dslService = GlobalDslService.of(gradle)

  // One instance of this per project
  internal val issueHandler: IssueHandler = objects.newInstance(dslService)

  internal val useTypesafeProjectAccessors: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

  // Only one instance of each of these is allowed globally, so we delegate to the build service
  internal val abiHandler: AbiHandler = dslService.get().abiHandler
  internal val dependenciesHandler: DependenciesHandler = dslService.get().dependenciesHandler
  internal val reportingHandler: ReportingHandler = dslService.get().reportingHandler
  internal val usageHandler: UsageHandler = dslService.get().usageHandler

  private val adviceOutput = objects.fileProperty()
  private var postProcessingTask: TaskProvider<out AbstractPostProcessingTask>? = null

  internal var forceAppProject = false

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
  @Suppress("MemberVisibilityCanBePrivate") // explicit API
  fun adviceOutput(): RegularFileProperty = adviceOutput

  /**
   * Whether to force the project being treated as an app project even if only the `java` plugin is applied.
   */
  fun app() {
    forceAppProject = true
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
