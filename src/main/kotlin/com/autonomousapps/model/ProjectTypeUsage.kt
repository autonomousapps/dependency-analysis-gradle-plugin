// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.squareup.moshi.JsonClass

/**
 * Type usage information for a single project.
 *
 * This report shows which types (classes/interfaces) are used from each dependency,
 * enabling coupling analysis and complexity metrics.
 */
@JsonClass(generateAdapter = false)
public data class ProjectTypeUsage(
  /** The project path (e.g., ":app", ":feature-home"). */
  val projectPath: String,

  /** Summary statistics. */
  val summary: TypeUsageSummary,

  /** Types defined in this project that are used by itself. Map: className -> usageCount. */
  val internal: Map<String, Int>,

  /** Types used from project dependencies (other modules). Map: coordinates -> (className -> usageCount). */
  val projectDependencies: Map<String, Map<String, Int>>,

  /** Types used from external library dependencies. Map: coordinates -> (className -> usageCount). */
  val libraryDependencies: Map<String, Map<String, Int>>,
) : Comparable<ProjectTypeUsage> {

  override fun compareTo(other: ProjectTypeUsage): Int = projectPath.compareTo(other.projectPath)

  /**
   * Returns true if there are no usages tracked.
   */
  public fun isEmpty(): Boolean =
    internal.isEmpty() &&
    projectDependencies.isEmpty() &&
    libraryDependencies.isEmpty()
}

/**
 * Summary statistics about type usage in a project.
 */
@JsonClass(generateAdapter = false)
public data class TypeUsageSummary(
  /** Total number of unique types used (internal + external). */
  val totalTypes: Int,

  /** Total number of source files analyzed. */
  val totalFiles: Int,

  /** Number of types defined and used within this project. */
  val internalTypes: Int,

  /** Number of project dependencies (other modules). */
  val projectDependencies: Int,

  /** Number of external library dependencies. */
  val libraryDependencies: Int,
)
