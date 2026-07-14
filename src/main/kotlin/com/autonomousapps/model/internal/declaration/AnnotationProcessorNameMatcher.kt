// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.declaration

import com.autonomousapps.model.internal.ProjectType
import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.internal.utils.uncapitalizeSafely

internal class AnnotationProcessorNameMatcher(
  val kind: AnnotationProcessor,
  private val projectType: ProjectType,
  private val supportedSourceSetNames: Set<String>,
) {

  enum class AnnotationProcessor(val value: String) {
    KAPT("kapt"), ANNOTATION_PROCESSOR("annotationProcessor")
  }

  /** Map of source set name to configuration name for the given [annotation processor][kind]. */
  private val sourceSetNamesToConfigurationNames: Map<String, String> by unsafeLazy {
    supportedSourceSetNames.associateWith { sourceSetName ->
      if (kind == AnnotationProcessor.KAPT) {
        kind.value + computeKaptName(sourceSetName).capitalizeSafely()
      } else {
        sourceSetName + kind.value.capitalizeSafely()
      }
    }
  }

  /** For KMP and Kapt, we change `jvmTest` to `test`. */
  private fun computeKaptName(base: String): String = when (projectType) {
    ProjectType.KMP -> base.substringAfter("jvm").uncapitalizeSafely()
    else -> base
  }

  fun matches(configurationName: String): Boolean {
    // exact match, for main source
    return configurationName == kind.value
      // match non-main source (also exactly)
      || configurationName in sourceSetNamesToConfigurationNames.values
  }

  /**
   * Find the source set name from the configuration name. E.g.:
   *
   * * "kaptTest" -> "test"
   * * "testAnnotationProcessor"" -> "test"
   */
  fun slug(configurationName: String): String {
    return sourceSetNamesToConfigurationNames
      .entries
      .find { (_, c) -> configurationName == c }
      ?.key
      ?: error("No source set name associated with '$configurationName'.")
  }
}
