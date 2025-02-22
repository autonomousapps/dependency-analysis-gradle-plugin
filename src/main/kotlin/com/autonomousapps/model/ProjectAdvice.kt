// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.autonomousapps.internal.utils.LexicographicIterableComparator
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

  /** Returns true if this has no advice, nor any warnings. */
  fun isEmpty(): Boolean = dependencyAdvice.isEmpty()
    && pluginAdvice.isEmpty()
    && ModuleAdvice.isEmpty(moduleAdvice)
    && warning.isEmpty()

  /**
   * Returns true if this has any [dependency advice][dependencyAdvice], any [plugin advice][pluginAdvice], any
   * [module advice][moduleAdvice], or any [warnings][warning].
   */
  fun isNotEmpty(): Boolean = !isEmpty()

  /** Returns true if this [isEmpty] or contains only warnings. */
  fun isEmptyOrWarningOnly(): Boolean = isEmpty() || warning.isNotEmpty()

  override fun compareTo(other: ProjectAdvice): Int {
    return compareBy(ProjectAdvice::projectPath)
      .thenBy(LexicographicIterableComparator()) { it.dependencyAdvice }
      .thenBy(LexicographicIterableComparator()) { it.pluginAdvice }
      .thenBy(LexicographicIterableComparator()) { it.moduleAdvice }
      .thenBy(ProjectAdvice::warning)
      .thenBy(ProjectAdvice::shouldFail)
      .compare(this, other)
  }
}
