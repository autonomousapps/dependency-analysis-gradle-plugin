package com.autonomousapps.internal.android

import org.gradle.api.Project

internal class AndroidGradlePluginFactory(
  private val project: Project, private val agpVersion: String
) {

  private val agp = AgpVersion.version(agpVersion)

  fun newAdapter(): AndroidGradlePlugin = when {
    agp >= AgpVersion.version("4.2") -> AndroidGradlePlugin4_2(project, agpVersion)
    agp >= AgpVersion.version("4.1") -> AndroidGradlePlugin4_1(project, agpVersion)
    agp >= AgpVersion.version("4.0") -> AndroidGradlePlugin4_0(project, agpVersion)
    agp >= AgpVersion.version("3.6") -> AndroidGradlePlugin3_6(project, agpVersion)
    agp >= AgpVersion.version("3.5") -> AndroidGradlePlugin3_5(project, agpVersion)
    // Assume latest
    else -> AndroidGradlePlugin4_2(project, agpVersion)
  }
}
