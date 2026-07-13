// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.parse.advice

import com.autonomousapps.internal.cash.grammar.kotlindsl.model.DependencyDeclaration
import com.autonomousapps.model.Advice

internal class StandardAdviceFinder(
  advice: Set<Advice>,
  reversedDependencyMap: (String) -> String,
) : AbstractAdviceFinder(advice, reversedDependencyMap) {

  // nb: scope is ignored for this implementation
  override fun findAdvice(dependencyDeclaration: DependencyDeclaration, scope: String): Advice? {
    // TODO: the `*Identifier`s found for the project.dependencies.platform declaration are wildly wrong.
    val originalIdentifier = dependencyDeclaration.identifier.path.removeSurrounding("\"")
    val rawIdentifier = reversedDependencyMap(originalIdentifier)
    val normalizedIdentifier = normalizeTypeSafeProjectAccessor(originalIdentifier)

    return advice.find {
      // First match on GAV/identifier (check both original and normalized)
      (it.matchesIdentifier(rawIdentifier) || it.matchesIdentifier(normalizedIdentifier))
        // Then match on configuration
        && it.matchesConfiguration(dependencyDeclaration)
        // Then match on type (project, module, etc) with type-safe accessor support
        && it.matchesType(dependencyDeclaration, originalIdentifier)
        // Then match on capabilities
        && it.matchesCapabilities(dependencyDeclaration)
    }
  }

  private fun Advice.matchesConfiguration(dependencyDeclaration: DependencyDeclaration): Boolean {
    return fromConfiguration == dependencyDeclaration.configuration
  }
}
