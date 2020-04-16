package com.autonomousapps.advice

data class BuildHealth(
  val projectName: String,
  val dependencyAdvice: Set<Advice>,
  val pluginAdvice: Set<PluginAdvice>
) : Comparable<BuildHealth> {

  override fun compareTo(other: BuildHealth): Int {
    return projectName.compareTo(other.projectName)
  }
}
