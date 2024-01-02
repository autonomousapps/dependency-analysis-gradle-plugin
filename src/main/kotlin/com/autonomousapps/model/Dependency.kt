// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel
import java.io.File

@JsonClass(generateAdapter = false, generator = "sealed:type")
sealed class Dependency(
  open val coordinates: Coordinates,
  open val capabilities: Map<String, Capability>,
  // Can be empty because we don't get file for annotation processor dependencies.
  // This property is also unused and was only added speculatively, so maybe it doesn't matter
  open val files: List<File>
) : Comparable<Dependency> {
  override fun compareTo(other: Dependency): Int = coordinates.compareTo(other.coordinates)
}

@TypeLabel("project")
@JsonClass(generateAdapter = false)
data class ProjectDependency(
  override val coordinates: ProjectCoordinates,
  /** Map of [Capability] canonicalName to the capability. */
  override val capabilities: Map<String, Capability>,
  override val files: List<File>
) : Dependency(coordinates, capabilities, files)

@TypeLabel("module")
@JsonClass(generateAdapter = false)
data class ModuleDependency(
  override val coordinates: ModuleCoordinates,
  override val capabilities: Map<String, Capability>,
  override val files: List<File>
) : Dependency(coordinates, capabilities, files)

@TypeLabel("flat")
@JsonClass(generateAdapter = false)
data class FlatDependency(
  override val coordinates: FlatCoordinates,
  override val capabilities: Map<String, Capability>,
  override val files: List<File>
) : Dependency(coordinates, capabilities, files)

@TypeLabel("included_build")
@JsonClass(generateAdapter = false)
data class IncludedBuildDependency(
  override val coordinates: IncludedBuildCoordinates,
  override val capabilities: Map<String, Capability>,
  override val files: List<File>
) : Dependency(coordinates, capabilities, files)
