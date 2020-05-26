@file:Suppress("UnstableApiUsage", "unused")

package com.autonomousapps

import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider

abstract class AbstractExtension(private val objects: ObjectFactory) {

  private val adviceOutput = objects.fileProperty()

  internal fun storeAdviceOutput(provider: Provider<RegularFile>) {
    val output = objects.fileProperty().also {
      it.set(provider)
    }
    adviceOutput.set(output)
  }

  /**
   * Returns the output from the project-level holistic advice, produced by the
   * [AdviceSubprojectAggregationTask][com.autonomousapps.tasks.AdviceSubprojectAggregationTask].
   * This output is a [`Set<BuildHealth>`][com.autonomousapps.advice.ComprehensiveAdvice]
   *
   * Never null, but may _contain_ a null value. Use with [RegularFileProperty.getOrNull].
   */
  fun adviceOutput(): RegularFileProperty {
    return adviceOutput
  }
}
