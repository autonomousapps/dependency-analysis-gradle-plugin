// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.declaration

import com.autonomousapps.internal.utils.ModuleInfo
import com.autonomousapps.internal.utils.toIdentifiers
import com.autonomousapps.model.GradleVariantIdentification
import org.gradle.api.artifacts.Configuration

/** A simplified view of Gradle's [Configuration] class, primarily to enable unit tests. */
internal class SimplifiedConfiguration(
  val name: String,
  val isConsumable: Boolean,
  val isDeclarable: Boolean,
  val isResolvable: Boolean,
  val dependenciesProvider: () -> Set<Pair<ModuleInfo, GradleVariantIdentification>>,
) {
  companion object {
    fun of(configuration: Configuration): SimplifiedConfiguration {
      return SimplifiedConfiguration(
        name = configuration.name,
        isConsumable = configuration.isCanBeConsumed,
        isDeclarable = configuration.isCanBeDeclared,
        isResolvable = configuration.isCanBeResolved,
        dependenciesProvider = { configuration.dependencies.toIdentifiers() }
      )
    }
  }
}
