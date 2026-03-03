// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal

import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.FlatCoordinates
import com.autonomousapps.model.IncludedBuildCoordinates
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectCoordinates
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

@JsonClass(generateAdapter = false, generator = "sealed:type")
internal sealed class Dependency(
  open val coordinates: Coordinates,
  open val capabilities: Map<String, Capability>,
) : Comparable<Dependency> {
  override fun compareTo(other: Dependency): Int = coordinates.compareTo(other.coordinates)

  inline fun <reified T : Capability> findCapability(): T? {
    return capabilities[T::class.java.canonicalName] as? T?
  }
}

@TypeLabel("project")
@JsonClass(generateAdapter = false)
internal data class ProjectDependency(
  override val coordinates: ProjectCoordinates,
  /** Map of [Capability] canonicalName to the capability. */
  override val capabilities: Map<String, Capability>,
) : Dependency(coordinates, capabilities)

@TypeLabel("module")
@JsonClass(generateAdapter = false)
internal data class ModuleDependency(
  override val coordinates: ModuleCoordinates,
  override val capabilities: Map<String, Capability>,
) : Dependency(coordinates, capabilities)

@TypeLabel("flat")
@JsonClass(generateAdapter = false)
internal data class FlatDependency(
  override val coordinates: FlatCoordinates,
  override val capabilities: Map<String, Capability>,
) : Dependency(coordinates, capabilities)

@TypeLabel("included_build")
@JsonClass(generateAdapter = false)
internal data class IncludedBuildDependency(
  override val coordinates: IncludedBuildCoordinates,
  override val capabilities: Map<String, Capability>,
) : Dependency(coordinates, capabilities)
