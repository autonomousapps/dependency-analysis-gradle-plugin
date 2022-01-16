@file:Suppress("unused")

package com.autonomousapps

import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.mapToSet
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectAdvice
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * Extend this class to do custom post-processing of the [ComprehensiveAdvice] produced by this project.
 */
abstract class AbstractPostProcessingTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
  }

  @get:Input
  abstract val version1: Property<Boolean>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val input: RegularFileProperty

  private val isV1 by lazy { version1.get() }

  /**
   * V1.
   */
  fun comprehensiveAdvice(): ComprehensiveAdvice {
    return if (isV1) {
      input.fromJson()
    } else {
      val advice = projectAdvice()
      ComprehensiveAdvice(
        projectPath = advice.projectPath,
        dependencyAdvice = fromOldAdvice(advice.dependencyAdvice),
        pluginAdvice = advice.pluginAdvice,
        shouldFail = advice.shouldFail
      )
    }
  }

  /**
   * V2.
   */
  fun projectAdvice(): ProjectAdvice {
    check(!isV1) { "This method should not be called unless you're using v2" }
    return input.fromJson()
  }
}

private fun fromOldAdvice(advice: Collection<Advice>): Set<com.autonomousapps.advice.Advice> {
  return advice.mapToSet { fromOldAdvice(it) }
}

private fun fromOldAdvice(advice: Advice): com.autonomousapps.advice.Advice {
  val resolvedVersion = when (advice.coordinates) {
    is ModuleCoordinates -> advice.coordinates.resolvedVersion
    else -> null
  }
  val dependency = Dependency(
    identifier = advice.coordinates.identifier,
    resolvedVersion = resolvedVersion,
    configurationName = advice.fromConfiguration
  )
  return com.autonomousapps.advice.Advice(
    dependency = dependency,
    fromConfiguration = advice.fromConfiguration,
    toConfiguration = advice.toConfiguration
  )
}
