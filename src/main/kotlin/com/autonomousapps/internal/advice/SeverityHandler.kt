// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.extension.Behavior
import com.autonomousapps.extension.Fail
import com.autonomousapps.internal.DependencyScope
import com.autonomousapps.internal.utils.filterToSet
import com.autonomousapps.internal.utils.lowercase
import com.autonomousapps.model.Advice
import com.autonomousapps.model.DuplicateClass
import com.autonomousapps.model.ModuleAdvice
import com.autonomousapps.model.PluginAdvice
import com.autonomousapps.model.Advice as DependencyAdvice

/** Given the set of all behaviors, determine whether the analysis should fail the build. */
internal class SeverityHandler(
  private val anyBehavior: Pair<Behavior, List<Behavior>>,
  private val unusedDependenciesBehavior: Pair<Behavior, List<Behavior>>,
  private val usedTransitiveDependenciesBehavior: Pair<Behavior, List<Behavior>>,
  private val incorrectConfigurationBehavior: Pair<Behavior, List<Behavior>>,
  private val compileOnlyBehavior: Pair<Behavior, List<Behavior>>,
  private val unusedProcsBehavior: Pair<Behavior, List<Behavior>>,
  private val duplicateClassWarningsBehavior: Pair<Behavior, List<Behavior>>,
  private val redundantPluginsBehavior: Behavior,
  private val moduleStructureBehavior: Behavior,
) {

  fun shouldFailDeps(advice: Set<DependencyAdvice>): Boolean {
    return shouldFailForAdvice(anyBehavior, advice) ||
      shouldFailForAdvice(unusedDependenciesBehavior, advice.filterToSet { it.isRemove() }) ||
      shouldFailForAdvice(usedTransitiveDependenciesBehavior, advice.filterToSet { it.isAdd() }) ||
      shouldFailForAdvice(incorrectConfigurationBehavior, advice.filterToSet { it.isChange() }) ||
      shouldFailForAdvice(compileOnlyBehavior, advice.filterToSet { it.isCompileOnly() }) ||
      shouldFailForAdvice(unusedProcsBehavior, advice.filterToSet { it.isProcessor() })
  }

  fun shouldFailPlugins(pluginAdvice: Set<PluginAdvice>): Boolean {
    return (redundantPluginsBehavior.isFail() || anyBehavior.first.isFail()) && pluginAdvice.isNotEmpty()
  }

  fun shouldFailModuleStructure(moduleAdvice: Set<ModuleAdvice>): Boolean {
    return (moduleStructureBehavior.isFail() || anyBehavior.first.isFail()) && ModuleAdvice.isNotEmpty(moduleAdvice)
  }

  fun shouldFailDuplicateClasses(duplicateClasses: Set<DuplicateClass>): Boolean {
    return shouldFailForDuplicateClasses(duplicateClassWarningsBehavior, duplicateClasses)
  }

  private fun shouldFailForAdvice(
    spec: Pair<Behavior, List<Behavior>>,
    advice: Set<DependencyAdvice>,
  ): Boolean {
    // Seed the "global" advice with the set of all possible advice. Later on we'll drain this set as elements of it are
    // "consumed" by sourceSet-specific behaviors.
    val globalAdvice = advice.toMutableSet()

    val bySourceSets: (Advice) -> Boolean = { a ->
      // These are the custom behaviors, if any, associated with the source sets represented by this advice.
      val behaviors = spec.second.filter { b ->
        val from = a.fromConfiguration?.let { DependencyScope.sourceSetName(it) }
        val to = a.toConfiguration?.let { DependencyScope.sourceSetName(it) }

        b.sourceSetName == from || b.sourceSetName == to
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

  private fun shouldFailForDuplicateClasses(
    spec: Pair<Behavior, List<Behavior>>,
    duplicateClasses: Set<DuplicateClass>,
  ): Boolean {
    // Seed the "global" warnings with the set of all possible warnings. Later on we'll drain this set as elements of it
    // are  "consumed" by sourceSet-specific behaviors.
    val globalAdvice = duplicateClasses.toMutableSet()

    val bySourceSets: (DuplicateClass) -> Boolean = { d ->
      // These are the custom behaviors, if any, associated with the source sets represented by this warning.
      val behaviors = spec.second.filter { b ->
        b.sourceSetName == DependencyScope.sourceSetName(d.classpathName)
      }

      // Looking for a match between sourceSet-specific behavior and warning.
      var shouldFail = false
      behaviors.forEach { b ->
        val s = b.sourceSetName.lowercase()
        val from = d.classpathName.lowercase().startsWith(s)

        if (from) {
          shouldFail = shouldFail || b.isFail()
          globalAdvice.remove(d)
        }
      }

      shouldFail
    }

    // If all advice is sourceSet-specific, then globalAdvice will be empty.
    return duplicateClasses.any(bySourceSets) || (spec.first.isFail() && globalAdvice.isNotEmpty())
  }

  private fun Behavior.isFail(): Boolean = this is Fail
}
