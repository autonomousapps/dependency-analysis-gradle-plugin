// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.transform

import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.internal.ProjectType
import com.autonomousapps.model.internal.declaration.ConfigurationNames
import com.autonomousapps.model.internal.declaration.Declaration
import com.autonomousapps.model.internal.intermediates.Usage

internal class TransformFactory(
  private val coordinates: Coordinates,
  private val declarations: Set<Declaration>,
  private val dependencyGraph: Map<String, DependencyGraphView>,
  private val buildPath: String,
  private val configurationNames: ConfigurationNames,
  private val explicitSourceSets: Set<String> = emptySet(),
  private val isKaptApplied: Boolean = false,
) {

  fun of(projectType: ProjectType): Usage.Transform = when (projectType) {
    ProjectType.ANDROID -> newAndroidTransform()
    ProjectType.JVM -> newJvmTransform()
    ProjectType.KMP -> newKmpTransform()
  }

  private fun newAndroidTransform(): AndroidTransform {
    return AndroidTransform(
      coordinates = coordinates,
      declarations = declarations,
      explicitSourceSets = explicitSourceSets,
      configurationNames = configurationNames,
      buildPath = buildPath,
      dependencyGraph = dependencyGraph,
      isKaptApplied = isKaptApplied,
    )
  }

  private fun newJvmTransform(): JvmTransform {
    return JvmTransform(
      coordinates = coordinates,
      declarations = declarations,
      explicitSourceSets = explicitSourceSets,
      configurationNames = configurationNames,
      buildPath = buildPath,
      dependencyGraph = dependencyGraph,
      isKaptApplied = isKaptApplied,
    )
  }

  private fun newKmpTransform(): KmpTransform {
    return KmpTransform(
      coordinates = coordinates,
      declarations = declarations,
      explicitSourceSets = explicitSourceSets,
      configurationNames = configurationNames,
      buildPath = buildPath,
      dependencyGraph = dependencyGraph,
      isKaptApplied = isKaptApplied,
    )
  }
}
