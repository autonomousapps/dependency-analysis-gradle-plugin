// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.android

import org.gradle.api.Project

internal class AndroidGradlePluginFactory(
  private val project: Project, private val agpVersion: String
) {

  private val agp = AgpVersion.version(agpVersion)

  fun newAdapter(): AndroidGradlePlugin = when {
    agp >= AgpVersion.version("4.2") -> AndroidGradlePlugin4_2(project, agpVersion)
    // Assume latest
    else -> AndroidGradlePlugin4_2(project, agpVersion)
  }
}
