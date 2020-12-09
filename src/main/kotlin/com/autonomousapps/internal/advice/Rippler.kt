package com.autonomousapps.internal.advice

import com.autonomousapps.advice.*

internal class Rippler(private val buildHealth: List<ComprehensiveAdvice>) {

  fun computeRipples(): List<Ripple> {
    val upgrades = mutableListOf<DownstreamImpact>()
    val downgrades = mutableListOf<UpstreamSource>()

    // Iterate over all of buildHealth and find two things:
    // 1. Transitively-used dependencies which are supplied by upstream/dependency projects.
    // 2. Any "downgrade" of a dependency.
    buildHealth.forEach { compAdvice ->
      compAdvice.dependencyAdvice.forEach { advice ->
        if (advice.isAdd()) {
          advice.parents
            ?.filter { it.identifier.startsWith(":") }
            ?.forEach { projDep ->
              upgrades.add(DownstreamImpact(
                sourceProjectPath = projDep.identifier,
                impactProjectPath = compAdvice.projectPath,
                providedDependency = advice.dependency,
                toConfiguration = advice.toConfiguration
              ))
            }
        }
        if (advice.isDowngrade()) {
          downgrades.add(UpstreamSource(
            projectPath = compAdvice.projectPath,
            providedDependency = advice.dependency,
            fromConfiguration = advice.fromConfiguration,
            toConfiguration = advice.toConfiguration
          ))
        }
      }
    }

    // With the above two items, we can now:
    // 3. Find all the downgrades that are transitively-used by dependents, and note them as "ripples".
    val ripples = mutableListOf<Ripple>()
    downgrades.forEach { downgrade ->
      upgrades.filter { upgrade ->
        upgrade.sourceProjectPath == downgrade.projectPath
          && upgrade.providedDependency == downgrade.providedDependency
      }.forEach { impact ->
        ripples.add(Ripple(
          upstreamSource = downgrade,
          downstreamImpact = impact
        ))
      }
    }
    return ripples
  }
}

/**
 * If this is advice to remove or downgrade an api-like dependency.
 */
private fun Advice.isDowngrade(): Boolean {
  return (isRemove() || isChange() || isCompileOnly())
    && dependency.configurationName?.endsWith("api", ignoreCase = true) == true
}
