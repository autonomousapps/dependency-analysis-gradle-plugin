// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import javax.inject.Inject

/**
 * Configuration for the runtime-usage package heuristic.
 *
 * The runtime-usage filter suppresses "remove" advice for dependencies that are detected as
 * runtime-used (e.g. via Spring DI or Liquibase). Its primary matching strategy uses real
 * type-usage data. As a fallback, an optional package-naming heuristic can match a dependency's
 * coordinates against the packages of runtime-referenced classes. This heuristic is **disabled by
 * default** because it is naming-convention-dependent and can over-suppress; enable it only when
 * your project's module/artifact names map predictably onto package names.
 *
 * Example usage:
 * ```
 * dependencyAnalysis {
 *   runtimeUsage {
 *     packageHeuristic {
 *       enabled(true)
 *       // Strip these prefixes from artifact names before matching (e.g. "appian-", "eng-").
 *       stripPrefixes("appian-", "eng-")
 *       // Additional suffixes to strip (defaults already include -java, -api, -impl, -core, -db,
 *       // -contracts, -client).
 *       stripSuffixes("-server")
 *       // For project dependencies (":group:module:artifact"), skip this many leading path
 *       // segments (e.g. 1 to skip an "appian-libraries" grouping segment).
 *       skipLeadingSegments(1)
 *       // Words to ignore when producing match segments (e.g. "appian", "libraries").
 *       stopwords("appian", "libraries")
 *       // Minimum length for a segment to be considered (default 4).
 *       minSegmentLength(4)
 *     }
 *   }
 * }
 * ```
 */
public abstract class RuntimeUsageHandler @Inject constructor(
  private val objects: ObjectFactory,
) {

  internal val packageHeuristic: PackageHeuristicHandler =
    objects.newInstance(PackageHeuristicHandler::class.java)

  /** Configure the optional package-naming heuristic. */
  public fun packageHeuristic(action: org.gradle.api.Action<PackageHeuristicHandler>) {
    action.execute(packageHeuristic)
  }

  internal fun config(): Config {
    val config = objects.newInstance(Config::class.java)
    config.enabled.set(packageHeuristic.enabled)
    config.stripPrefixes.set(packageHeuristic.stripPrefixes)
    config.stripSuffixes.set(packageHeuristic.stripSuffixes)
    config.skipLeadingSegments.set(packageHeuristic.skipLeadingSegments)
    config.stopwords.set(packageHeuristic.stopwords)
    config.minSegmentLength.set(packageHeuristic.minSegmentLength)
    return config
  }

  public abstract class PackageHeuristicHandler @Inject constructor(
    objects: ObjectFactory,
  ) {

    internal val enabled: Property<Boolean> =
      objects.property(Boolean::class.java).convention(false)

    internal val stripPrefixes: SetProperty<String> =
      objects.setProperty(String::class.java)

    internal val stripSuffixes: SetProperty<String> =
      objects.setProperty(String::class.java).convention(DEFAULT_STRIP_SUFFIXES)

    internal val skipLeadingSegments: Property<Int> =
      objects.property(Int::class.java).convention(0)

    internal val stopwords: SetProperty<String> =
      objects.setProperty(String::class.java)

    internal val minSegmentLength: Property<Int> =
      objects.property(Int::class.java).convention(4)

    /** Enable or disable the package heuristic. Disabled by default. */
    public fun enabled(enabled: Boolean) {
      this.enabled.set(enabled)
    }

    /** Prefixes to strip from external-dependency artifact names before matching. */
    public fun stripPrefixes(vararg prefixes: String) {
      require(prefixes.isNotEmpty()) { "Must provide at least one prefix." }
      stripPrefixes.addAll(prefixes.toList())
    }

    /** Additional suffixes to strip from dependency artifact/module names before matching. */
    public fun stripSuffixes(vararg suffixes: String) {
      require(suffixes.isNotEmpty()) { "Must provide at least one suffix." }
      stripSuffixes.addAll(suffixes.toList())
    }

    /**
     * For project dependencies (`:group:module:artifact`), skip this many leading path segments
     * before extracting match segments.
     */
    public fun skipLeadingSegments(count: Int) {
      require(count >= 0) { "skipLeadingSegments must be >= 0." }
      skipLeadingSegments.set(count)
    }

    /** Words to ignore when producing match segments. */
    public fun stopwords(vararg words: String) {
      require(words.isNotEmpty()) { "Must provide at least one stopword." }
      stopwords.addAll(words.toList())
    }

    /** Minimum length for a segment to be considered a match candidate. Default 4. */
    public fun minSegmentLength(length: Int) {
      require(length >= 1) { "minSegmentLength must be >= 1." }
      minSegmentLength.set(length)
    }

    internal companion object {
      // Common JVM/Gradle artifact suffixes that don't contribute to the package name.
      internal val DEFAULT_STRIP_SUFFIXES: Set<String> = setOf(
        "-java", "-api", "-impl", "-core", "-db", "-contracts", "-client",
      )
    }
  }

  public interface Config {
    @get:Input public val enabled: Property<Boolean>
    @get:Input public val stripPrefixes: SetProperty<String>
    @get:Input public val stripSuffixes: SetProperty<String>
    @get:Input public val skipLeadingSegments: Property<Int>
    @get:Input public val stopwords: SetProperty<String>
    @get:Input public val minSegmentLength: Property<Int>
  }
}
