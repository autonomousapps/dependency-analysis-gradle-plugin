// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import javax.inject.Inject

/**
 * Restricts which dependencies appear in build-health advice by matching their coordinate
 * identifier against regular expressions. Applies to all advice types (add / remove / change).
 *
 * A dependency's identifier is its coordinates string, e.g. `:foo:bar` for a project dependency or
 * `com.example:widget` for an external module.
 *
 * Matching semantics:
 * - If [includeCoordinates] is empty, all coordinates are included by default.
 * - If [includeCoordinates] is non-empty, a coordinate must match at least one include pattern.
 * - Any coordinate matching an [excludeCoordinates] pattern is then removed (exclude wins).
 *
 * Example — report only "internal" dependencies (project deps and a given group):
 * ```
 * dependencyAnalysis {
 *   advice {
 *     includeCoordinates("^:.*", "^com\\.example\\..*")
 *   }
 * }
 * ```
 *
 * Example — report everything except a noisy third-party group:
 * ```
 * dependencyAnalysis {
 *   advice {
 *     excludeCoordinates("^org\\.springframework.*")
 *   }
 * }
 * ```
 *
 * Also controls [transitiveDepth]: how many downstream hops the transitive-exposure check follows
 * when deciding whether a project dependency is still needed by consumers (default `1`).
 */
public abstract class AdviceFilterHandler @Inject constructor(
  private val objects: ObjectFactory,
) {

  internal val includeCoordinates: ListProperty<String> = objects.listProperty(String::class.java)
  internal val excludeCoordinates: ListProperty<String> = objects.listProperty(String::class.java)

  // Number of downstream hops to traverse when checking transitive exposure. 1 = direct consumers
  // only (default, preserves historical behavior). Higher values catch longer A->B->C chains at the
  // cost of more false-negative suppression risk.
  internal val transitiveDepth: org.gradle.api.provider.Property<Int> =
    objects.property(Int::class.java).convention(1)

  /**
   * Only include dependencies whose coordinate identifier matches at least one of these regex
   * patterns. If never set, all coordinates are included.
   */
  public fun includeCoordinates(
    @org.intellij.lang.annotations.Language("RegExp") vararg patterns: String
  ) {
    require(patterns.isNotEmpty()) { "Must provide at least one pattern." }
    includeCoordinates.addAll(patterns.toList())
  }

  /**
   * Exclude dependencies whose coordinate identifier matches any of these regex patterns. Applied
   * after [includeCoordinates].
   */
  public fun excludeCoordinates(
    @org.intellij.lang.annotations.Language("RegExp") vararg patterns: String
  ) {
    require(patterns.isNotEmpty()) { "Must provide at least one pattern." }
    excludeCoordinates.addAll(patterns.toList())
  }

  /**
   * How many downstream hops to traverse when checking whether a project dependency is transitively
   * exposed to consumers. `1` (default) checks only direct consumers; higher values follow longer
   * `A -> B -> C` chains. Must be >= 1.
   */
  public fun transitiveDepth(depth: Int) {
    require(depth >= 1) { "transitiveDepth must be >= 1." }
    transitiveDepth.set(depth)
  }

  internal fun config(): Config {
    val config = objects.newInstance(Config::class.java)
    config.includeCoordinates.set(includeCoordinates)
    config.excludeCoordinates.set(excludeCoordinates)
    config.transitiveDepth.set(transitiveDepth)
    return config
  }

  public interface Config {
    @get:Input public val includeCoordinates: ListProperty<String>
    @get:Input public val excludeCoordinates: ListProperty<String>
    @get:Input public val transitiveDepth: org.gradle.api.provider.Property<Int>
  }
}
