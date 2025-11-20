// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.declaration

import com.autonomousapps.internal.utils.ModuleInfo
import com.autonomousapps.model.GradleVariantIdentification
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.tasks.Input

/** Public because task input. Internal implementation detail. */
public class DeclarationContainer(
  @get:Input public val mapping: Map<String, Set<Pair<ModuleInfo, GradleVariantIdentification>>>,
) {

  internal companion object {
    fun of(
      configurations: ConfigurationContainer,
      configurationNames: ConfigurationNames,
      shouldAnalyzeTests: Boolean,
    ): DeclarationContainer {
      return of(
        configurations = configurations.asSequence().map { c -> SimplifiedConfiguration.of(c) },
        configurationNames = configurationNames,
        shouldAnalyzeTests = shouldAnalyzeTests,
      )
    }

    fun of(
      configurations: Sequence<SimplifiedConfiguration>,
      configurationNames: ConfigurationNames,
      shouldAnalyzeTests: Boolean,
    ): DeclarationContainer {
      val mapping = getDependencyBuckets(configurations, configurationNames, shouldAnalyzeTests)
        .associateBy { c -> c.name }
        .map { (name, c) -> name to c.dependenciesProvider() }
        .toMap()

      return DeclarationContainer(mapping)
    }

    /** Filters the full set of [configurations], returning just those that are "dependency buckets" (declar`ables). */
    private fun getDependencyBuckets(
      configurations: Sequence<SimplifiedConfiguration>,
      configurationNames: ConfigurationNames,
      shouldAnalyzeTests: Boolean,
    ): Sequence<SimplifiedConfiguration> {
      val seq = configurations.filter { c -> configurationNames.isDependencyBucket(c.name) }

      return if (shouldAnalyzeTests) seq
      else seq.filterNot { it.name.startsWith("test") }
    }
  }
}
