// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.autonomousapps.internal.utils.MapSetComparator
import com.squareup.moshi.JsonClass
import java.util.*

/**
 * Simplified type usage information for a single project, aggregated across all variants / source sets.
 *
 * This report shows which types (classes/interfaces) are used from each dependency, enabling coupling analysis and
 * complexity metrics.
 *
 * TODO(tsr): consider adding Android res usages here as well.
 * TODO(tsr): consider making this public.
 *
 * @see [com.autonomousapps.model.ProjectTypeUsage]
 * @see [PublicTypeUsage]
 */
@JsonClass(generateAdapter = false)
internal data class AggregateTypeUsageReport(
  /** The project path (e.g., ":app", ":feature-home"). */
  val projectPath: String,

  /** Types defined in this project that are used by itself. */
  val internal: Set<String>,

  /** Types used from project dependencies (other modules). Map: dependencyIdentifier -> classNames. */
  val projectDependencies: Map<String, Set<String>>,

  /** Types used from external library dependencies. Map: dependencyIdentifier -> classNames. */
  val libraryDependencies: Map<String, Set<String>>,

  /** Types used that could not be resolved to any dependency. */
  val unknownDependencies: Set<String> = emptySet(),
) : Comparable<AggregateTypeUsageReport> {

  override fun compareTo(other: AggregateTypeUsageReport): Int {
    return compareBy(AggregateTypeUsageReport::projectPath)
      .thenBy(LexicographicIterableComparator()) { it.internal }
      .thenBy(MapSetComparator()) { it.projectDependencies }
      .thenBy(MapSetComparator()) { it.libraryDependencies }
      .thenBy(LexicographicIterableComparator()) { it.unknownDependencies }
      .compare(this, other)
  }

  fun isEmpty(): Boolean = internal.isEmpty() && projectDependencies.isEmpty() && libraryDependencies.isEmpty()

  internal class Builder {
    var projectPath: String? = null
    val internal = sortedSetOf<String>()
    val projectDependencies = sortedMapOf<String, SortedSet<String>>()
    val libraryDependencies = sortedMapOf<String, SortedSet<String>>()
    val unknownDependencies = sortedSetOf<String>()

    fun build(): AggregateTypeUsageReport {
      return AggregateTypeUsageReport(
        projectPath = requireNotNull(projectPath),
        internal = internal,
        projectDependencies = projectDependencies,
        libraryDependencies = libraryDependencies,
        unknownDependencies = unknownDependencies,
      )
    }
  }
}
