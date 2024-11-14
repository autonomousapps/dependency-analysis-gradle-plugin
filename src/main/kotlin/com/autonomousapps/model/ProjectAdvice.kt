// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.squareup.moshi.JsonClass

/** Collection of all advice for a single project, across all variants. */
@JsonClass(generateAdapter = false)
data class ProjectAdvice(
  val projectPath: String,
  val dependencyAdvice: Set<Advice> = emptySet(),
  val pluginAdvice: Set<PluginAdvice> = emptySet(),
  val moduleAdvice: Set<ModuleAdvice> = emptySet(),
  val warning: Warning = Warning.empty(),
  /** True if there is any advice in a category for which the user has declared they want the build to fail. */
  val shouldFail: Boolean = false
) : Comparable<ProjectAdvice> {

  fun isEmpty(): Boolean = dependencyAdvice.isEmpty() && pluginAdvice.isEmpty() && warning.isEmpty()
  fun isNotEmpty(): Boolean = !isEmpty()

  // TODO(tsr): this compareTo function violates the Comparable contract by only considering one field.
  override fun compareTo(other: ProjectAdvice): Int = projectPath.compareTo(other.projectPath)
}
