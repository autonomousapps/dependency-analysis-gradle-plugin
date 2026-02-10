// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

/**
 * Configuration for type usage analysis filtering.
 *
 * Example usage:
 * ```
 * dependencyAnalysis {
 *   typeUsage {
 *     excludePackages("kotlin.jvm.internal", "android")
 *     excludeTypes("kotlin.Unit", "dagger.internal.Factory")
 *     excludeRegex(".*_Factory$", ".*Companion$")
 *   }
 * }
 * ```
 */
public abstract class TypeUsageHandler @Inject constructor(
  objects: ObjectFactory
) {

  // Excluded package prefixes
  internal val excludedPackages: SetProperty<String> = objects.setProperty(String::class.java)
    .convention(setOf(
      "kotlin.jvm.internal",
      "androidx.compose.runtime.internal",
      "kotlin.jvm.functions",
      "android"
    ))

  // Excluded specific types
  internal val excludedTypes: SetProperty<String> = objects.setProperty(String::class.java)
    .convention(setOf(
      "kotlin.Unit",
      "kotlin.coroutines.Continuation",
      "kotlin.reflect.KClass",
      "kotlin.Lazy",
      "dagger.internal.Factory"
    ))

  // Regex patterns for flexible matching
  internal val excludedRegexPatterns: ListProperty<String> = objects.listProperty(String::class.java)
    .convention(listOf(
      "^anvil\\.hint\\..*",
      ".*_Factory$",
      ".*_Factory\\$.*",
      ".*_MembersInjector$",
      ".*_MembersInjector\\$.*"
    ))

  /**
   * Exclude types by package prefix.
   *
   * Example:
   * ```
   * excludePackages("com.example.internal", "kotlin.jvm.internal")
   * ```
   */
  public fun excludePackages(vararg packages: String) {
    excludedPackages.addAll(packages.toList())
  }

  /**
   * Exclude specific types by fully-qualified name.
   *
   * Example:
   * ```
   * excludeTypes("kotlin.Unit", "com.example.GeneratedClass")
   * ```
   */
  public fun excludeTypes(vararg types: String) {
    excludedTypes.addAll(types.toList())
  }

  /**
   * Exclude types matching regex patterns.
   *
   * Example:
   * ```
   * excludeRegex(".*Companion$", ".*\\.internal\\..*")
   * ```
   */
  public fun excludeRegex(
    @org.intellij.lang.annotations.Language("RegExp") vararg patterns: String
  ) {
    excludedRegexPatterns.addAll(patterns.toList())
  }
}
