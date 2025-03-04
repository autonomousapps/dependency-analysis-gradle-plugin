package com.autonomousapps.internal.utils

import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ModuleCoordinates
import org.gradle.api.file.ConfigurableFileCollection

fun ConfigurableFileCollection.dependencyCoordinates(): List<ModuleCoordinates> =
  this.files
    .flatMap { it.readLines() }
    .map {
      val external = Coordinates.of(it)
      check(external is ModuleCoordinates) { "ModuleCoordinates expected. Was $it." }
      external
    }
