// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.internal.utils.getJsonAdapter
import com.autonomousapps.internal.utils.jsonWriter
import com.autonomousapps.model.AndroidScore
import com.autonomousapps.model.BuildHealth.AndroidScoreMetrics
import com.autonomousapps.model.ProjectAdvice
import java.io.File

/**
 * Writes a `BuildHealth` document one [ProjectAdvice] at a time.

 */
internal class BuildHealthWriter(private val output: File) {

  /**
   * Writes [advice] to [output], invoking [onNonEmpty] for each element that carries advice, in iteration order.
   * Returns true if any project asked the build to fail.
   */
  fun write(
    advice: Sequence<ProjectAdvice>,
    projectCount: Int,
    onNonEmpty: (ProjectAdvice) -> Unit = {},
  ): Boolean {
    var shouldFail = false
    var unusedDependencies = 0
    var undeclaredDependencies = 0
    var misDeclaredDependencies = 0
    var compileOnlyDependencies = 0
    var runtimeOnlyDependencies = 0
    var processorDependencies = 0
    val androidMetricsBuilder = AndroidScoreMetrics.Builder()

    val adviceAdapter = getJsonAdapter<ProjectAdvice>()
    val metricsAdapter = getJsonAdapter<AndroidScoreMetrics>()

    output.jsonWriter().use { writer ->
      writer.beginObject()
      writer.name("projectAdvice").beginArray()

      advice.forEach { projectAdvice ->
        adviceAdapter.toJson(writer, projectAdvice)

        if (projectAdvice.isNotEmpty()) {
          shouldFail = shouldFail || projectAdvice.shouldFail

          projectAdvice.dependencyAdvice.forEach {
            when {
              it.isRemove() -> unusedDependencies++
              it.isAdd() -> undeclaredDependencies++
              it.isChange() -> misDeclaredDependencies++
              it.isCompileOnly() -> compileOnlyDependencies++
              it.isChangeToRuntimeOnly() -> runtimeOnlyDependencies++
              it.isProcessor() -> processorDependencies++
            }
          }
          projectAdvice.moduleAdvice.filterIsInstance<AndroidScore>().forEach {
            if (it.shouldBeJvm()) {
              androidMetricsBuilder.shouldBeJvmCount++
            } else if (it.couldBeJvm()) {
              androidMetricsBuilder.couldBeJvmCount++
            }
          }

          onNonEmpty(projectAdvice)
        }
      }

      writer.endArray()

      writer.name("shouldFail").value(shouldFail)
      writer.name("projectCount").value(projectCount.toLong())
      writer.name("unusedCount").value(unusedDependencies.toLong())
      writer.name("undeclaredCount").value(undeclaredDependencies.toLong())
      writer.name("misDeclaredCount").value(misDeclaredDependencies.toLong())
      writer.name("compileOnlyCount").value(compileOnlyDependencies.toLong())
      writer.name("runtimeOnlyCount").value(runtimeOnlyDependencies.toLong())
      writer.name("processorCount").value(processorDependencies.toLong())
      writer.name("androidScoreMetrics")
      metricsAdapter.toJson(writer, androidMetricsBuilder.build())

      writer.endObject()
    }

    return shouldFail
  }
}
