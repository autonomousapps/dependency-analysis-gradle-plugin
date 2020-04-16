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

  override fun compareTo(other: BuildHealth): Int {
    return projectPath.compareTo(other.projectPath)
  }
}

/**
 * For a single project.
 */
@Incubating
data class ComprehensiveAdvice(
  val dependencyAdvice: Set<Advice>,
  val pluginAdvice: Set<PluginAdvice>
)
