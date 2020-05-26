package com.autonomousapps.advice

import org.gradle.api.Incubating

/**
 * For the entire multi-project project.
 */
@Incubating
data class BuildHealth(
  val projectPath: String,
  val dependencyAdvice: Set<Advice>,
  val pluginAdvice: Set<PluginAdvice>
) : Comparable<BuildHealth> {

  fun isEmpty(): Boolean = dependencyAdvice.isEmpty() && pluginAdvice.isEmpty()
  fun isNotEmpty(): Boolean = !isEmpty()

  override fun compareTo(other: BuildHealth): Int {
    return projectPath.compareTo(other.projectPath)
  }
}

/**
 * Collection of all dependency-related advice and plugin-related advice for a single project,
 * across all variants.
 */
data class ComprehensiveAdvice(
  val dependencyAdvice: Set<Advice>,
  val pluginAdvice: Set<PluginAdvice>
)
