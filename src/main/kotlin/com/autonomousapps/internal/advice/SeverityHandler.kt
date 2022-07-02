package com.autonomousapps.internal.advice

import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.extension.Behavior
import com.autonomousapps.extension.Fail
import com.autonomousapps.model.ModuleAdvice
import com.autonomousapps.model.Advice as DependencyAdvice

internal class SeverityHandler(
  private val anyBehavior: Behavior,
  private val unusedDependenciesBehavior: Behavior,
  private val usedTransitiveDependenciesBehavior: Behavior,
  private val incorrectConfigurationBehavior: Behavior,
  private val compileOnlyBehavior: Behavior,
  private val unusedProcsBehavior: Behavior,
  private val redundantPluginsBehavior: Behavior,
  private val moduleStructureBehavior: Behavior,
) {

  fun shouldFailDeps(advice: Set<DependencyAdvice>): Boolean {
    return anyBehavior.isFail() && advice.isNotEmpty() ||
      unusedDependenciesBehavior.isFail() && advice.any { it.isRemove() } ||
      usedTransitiveDependenciesBehavior.isFail() && advice.any { it.isAdd() } ||
      incorrectConfigurationBehavior.isFail() && advice.any { it.isChange() } ||
      compileOnlyBehavior.isFail() && advice.any { it.isCompileOnly() } ||
      unusedProcsBehavior.isFail() && advice.any { it.isProcessor() }
  }

  fun shouldFailPlugins(pluginAdvice: Set<PluginAdvice>): Boolean {
    return (redundantPluginsBehavior.isFail() || anyBehavior.isFail()) && pluginAdvice.isNotEmpty()
  }

  fun shouldFailModuleStructure(moduleAdvice: Set<ModuleAdvice>): Boolean {
    return (moduleStructureBehavior.isFail() || anyBehavior.isFail()) && moduleAdvice.isNotEmpty()
  }

  private fun Behavior.isFail(): Boolean = this is Fail
}
