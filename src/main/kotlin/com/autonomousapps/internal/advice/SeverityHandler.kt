package com.autonomousapps.internal.advice

import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.extension.Behavior
import com.autonomousapps.extension.Fail
import com.autonomousapps.internal.utils.filterToSet
import com.autonomousapps.internal.utils.lowercase
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ModuleAdvice
import com.autonomousapps.model.Advice as DependencyAdvice

/** Given the set of all behaviors, determine whether the analysis should fail the build. */
internal class SeverityHandler(
  private val supportedSourceSets: Set<String>,
  private val anyBehavior: Pair<Behavior, List<Behavior>>,
  private val unusedDependenciesBehavior: Pair<Behavior, List<Behavior>>,
  private val usedTransitiveDependenciesBehavior: Pair<Behavior, List<Behavior>>,
  private val incorrectConfigurationBehavior: Pair<Behavior, List<Behavior>>,
  private val compileOnlyBehavior: Pair<Behavior, List<Behavior>>,
  private val unusedProcsBehavior: Pair<Behavior, List<Behavior>>,
  private val redundantPluginsBehavior: Behavior,
  private val moduleStructureBehavior: Behavior,
) {

  fun shouldFailDeps(advice: Set<DependencyAdvice>): Boolean {
    return shouldFailFor(anyBehavior, advice) ||
      shouldFailFor(unusedDependenciesBehavior, advice.filterToSet { it.isRemove() }) ||
      shouldFailFor(usedTransitiveDependenciesBehavior, advice.filterToSet { it.isAdd() }) ||
      shouldFailFor(incorrectConfigurationBehavior, advice.filterToSet { it.isChange() }) ||
      shouldFailFor(compileOnlyBehavior, advice.filterToSet { it.isCompileOnly() }) ||
      shouldFailFor(unusedProcsBehavior, advice.filterToSet { it.isProcessor() })
  }

  private fun shouldFailFor(
    spec: Pair<Behavior, List<Behavior>>,
    advice: Set<DependencyAdvice>,
  ): Boolean {
    // Seed the "global" advice with the set of all possible advice. Later on we'll drain this set as elements of it are
    // "consumed" by sourceSet-specific behaviors.
    val globalAdvice = advice.toMutableSet()

    val bySourceSets: (Advice) -> Boolean = { a ->
      // These are the source sets represented in this advice. Might be empty if it is for the main source set.
      val adviceSourceSets = supportedSourceSets
        .map { it.lowercase() }
        .filter { s ->
          val from = a.fromConfiguration?.lowercase()?.startsWith(s) == true
          val to = a.toConfiguration?.lowercase()?.startsWith(s) == true
          from || to
        }

      // These are the behaviors, if any, specific to non-main source sets.
      val behaviors = spec.second.filter { b ->
        b.sourceSetName.lowercase() in adviceSourceSets
      }

      // Looking for a match between sourceSet-specific behavior and advice.
      var shouldFail = false
      behaviors.forEach { b ->
        val s = b.sourceSetName.lowercase()
        val from = a.fromConfiguration?.lowercase()?.startsWith(s) == true
        val to = a.toConfiguration?.lowercase()?.startsWith(s) == true

        if (from || to) {
          shouldFail = shouldFail || b.isFail()
          globalAdvice.remove(a)
        }
      }

      shouldFail
    }

    // If all advice is sourceSet-specific, then globalAdvice will be empty.
    return advice.any(bySourceSets) || (spec.first.isFail() && globalAdvice.isNotEmpty())
  }

  fun shouldFailPlugins(pluginAdvice: Set<PluginAdvice>): Boolean {
    return (redundantPluginsBehavior.isFail() || anyBehavior.first.isFail()) && pluginAdvice.isNotEmpty()
  }

  fun shouldFailModuleStructure(moduleAdvice: Set<ModuleAdvice>): Boolean {
    return (moduleStructureBehavior.isFail() || anyBehavior.first.isFail()) && ModuleAdvice.isNotEmpty(moduleAdvice)
  }

  private fun Behavior.isFail(): Boolean = this is Fail
}
