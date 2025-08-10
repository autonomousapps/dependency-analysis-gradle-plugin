// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ModuleCoordinates
import org.gradle.api.file.ConfigurableFileCollection

/**
 * Reads the set of all module coordinates for the the given artifact files.
 * Must only be called in a task action.
 */
internal fun ConfigurableFileCollection.dependencyCoordinates(): Set<ModuleCoordinates> =
  files.asSequence()
    .flatMap { it.readLines() }
    .map {
      val external = Coordinates.of(it)
      check(external is ModuleCoordinates) { "ModuleCoordinates expected. Was $it." }
      external
    }
    .toSortedSet()
