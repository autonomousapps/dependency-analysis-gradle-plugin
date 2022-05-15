package com.autonomousapps.model

import com.autonomousapps.advice.PluginAdvice
import com.squareup.moshi.JsonClass

/** Collection of all dependency- and plugin-related advice for a single project, across all variants. */
@JsonClass(generateAdapter = true)
data class ProjectAdvice(
  val projectPath: String,
  val dependencyAdvice: Set<Advice> = emptySet(),
  val pluginAdvice: Set<PluginAdvice> = emptySet(),
  /** True if there is any advice in a category for which the user has declared they want the build to fail. */
  val shouldFail: Boolean = false
) : Comparable<ProjectAdvice> {

  fun isEmpty(): Boolean = dependencyAdvice.isEmpty() && pluginAdvice.isEmpty()
  fun isNotEmpty(): Boolean = !isEmpty()

  override fun compareTo(other: ProjectAdvice): Int = projectPath.compareTo(other.projectPath)
}
