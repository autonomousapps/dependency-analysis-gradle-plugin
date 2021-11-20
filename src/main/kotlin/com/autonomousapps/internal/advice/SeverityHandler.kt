package com.autonomousapps.internal.advice

import com.autonomousapps.model.Advice as DependencyAdvice
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.extension.Behavior
import com.autonomousapps.extension.Fail

internal class SeverityHandler(
  private val anyBehavior: Behavior,
  private val unusedDependenciesBehavior: Behavior,
  private val usedTransitiveDependenciesBehavior: Behavior,
  private val incorrectConfigurationBehavior: Behavior,
  private val compileOnlyBehavior: Behavior,
  private val unusedProcsBehavior: Behavior,
  private val redundantPluginsBehavior: Behavior
) {

  fun shouldFailDeps(advice: Set<Advice>): Boolean {
    return anyBehavior.isFail() && advice.isNotEmpty() ||
      unusedDependenciesBehavior.isFail() && advice.any { it.isRemove() } ||
      usedTransitiveDependenciesBehavior.isFail() && advice.any { it.isAdd() } ||
      incorrectConfigurationBehavior.isFail() && advice.any { it.isChange() } ||
      compileOnlyBehavior.isFail() && advice.any { it.isCompileOnly() } ||
      unusedProcsBehavior.isFail() && advice.any { it.isProcessor() }
  }

  fun shouldFailDeps2(advice: Set<DependencyAdvice>): Boolean {
    return anyBehavior.isFail() && advice.isNotEmpty() ||
      unusedDependenciesBehavior.isFail() && advice.any { it.isRemove() } ||
      usedTransitiveDependenciesBehavior.isFail() && advice.any { it.isAdd() } ||
      incorrectConfigurationBehavior.isFail() && advice.any { it.isChange() } ||
      compileOnlyBehavior.isFail() && advice.any { it.isCompileOnly() } ||
      unusedProcsBehavior.isFail() && advice.any { it.isProcessor() }
  }

  fun shouldFailPlugins(pluginAdvice: Set<PluginAdvice>): Boolean {
    return redundantPluginsBehavior.isFail() && pluginAdvice.isNotEmpty()
  }

  private fun Behavior.isFail(): Boolean = this is Fail
}
