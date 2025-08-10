// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

import com.autonomousapps.model.ModuleCoordinates

internal fun Set<ModuleCoordinates>.toVersionCatalog(): String = buildString {
  appendLine("[libraries]")
  forEach { m: ModuleCoordinates ->
    appendLine("${m.toVersionCatalogAlias()} = { module = \"${m.identifier}\", version = \"${m.resolvedVersion}\" }")
  }
}
