// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.declaration

internal class Locator(private val declarationContainer: DeclarationContainer) {
  fun declarations(): Set<Declaration> {
    return declarationContainer.mapping.asSequence()
      .filter { (_, identifiers) -> identifiers.isNotEmpty() }
      .flatMap { (configurationName, identifiers) ->
        identifiers.map { id ->
          Declaration(
            identifier = id.first.identifier,
            version = id.first.version,
            configurationName = configurationName,
            gradleVariantIdentification = id.second
          )
        }
      }
      .toSortedSet()
  }
}
