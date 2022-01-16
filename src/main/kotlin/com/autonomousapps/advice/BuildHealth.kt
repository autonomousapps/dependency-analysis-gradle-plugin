package com.autonomousapps.advice

/** Collection of all dependency-related advice and plugin-related advice for a single project, across all variants. */
data class ComprehensiveAdvice(
  val projectPath: String,
  val dependencyAdvice: Set<Advice> = emptySet(),
  val pluginAdvice: Set<PluginAdvice> = emptySet(),
  /** True if there is any advice in a category for which the user has declared they want the build to fail. */
  val shouldFail: Boolean = false
) : Comparable<ComprehensiveAdvice> {

  fun isEmpty(): Boolean = dependencyAdvice.isEmpty() && pluginAdvice.isEmpty()
  fun isNotEmpty(): Boolean = !isEmpty()

  override fun compareTo(other: ComprehensiveAdvice): Int {
    return projectPath.compareTo(other.projectPath)
  }
}
