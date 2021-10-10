package com.autonomousapps.internal.advice

import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.advice.Dependency
import com.autonomousapps.advice.HasDependency
import com.autonomousapps.advice.TransitiveDependency
import com.autonomousapps.internal.*
import com.autonomousapps.internal.ServiceLoader
import com.autonomousapps.internal.advice.filter.FilterSpecBuilder
import com.autonomousapps.internal.advice.filter.KtxFilter
import com.autonomousapps.internal.advice.filter.DependencyBundleFilter
import com.autonomousapps.internal.utils.*
import com.autonomousapps.internal.utils.filterNoneMatchingSorted
import com.autonomousapps.internal.utils.filterToOrderedSet
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.internal.utils.mapToSet
import java.util.*

/**
 * Classes of advice:
 * 1. Declared dependencies which are not used: remove
 * 2. Undeclared (transitive) dependencies are are used: add
 * 3. `implementation` dependencies incorrectly declared `api`
 * 4. `api` dependencies incorrectly declared `implementation`.
 * 5. `compileOnly` dependencies incorrectly declared as `api` or `implementation`.
 *
 * [ignoreKtx] is applied to the remove-advice to filter (or not) ktx-related advice; supplied by
 * user. If true, we don't suggest removing an "unused" `-ktx` dependency iff any of its
 * dependencies are used (directly or transitively).
 */
internal class Advisor(
  private val usedVariantDependencies: Set<VariantDependency>,
  private val allComponents: Set<Component>,
  private val allComponentsWithTransitives: Set<ComponentWithTransitives>,
  private val unusedComponentsWithTransitives: Set<ComponentWithTransitives>,
  private val usedTransitiveComponents: Set<TransitiveComponent>,
  private val abiDeps: Set<Dependency>,
  private val allDeclaredDeps: Set<Dependency>,
  private val unusedProcs: Set<AnnotationProcessor>,
  private val serviceLoaders: Set<ServiceLoader>,
  private val dependencyBundles: Map<String, Set<Regex>>,
  private val ignoreKtx: Boolean = false
) {

  private val compileOnlyCandidates: Set<Component> = TreeSet<Component>()
  private val securityProviders: Set<Component> = TreeSet<Component>()
  private val lintJars: Set<Component> = TreeSet<Component>()

  init {
    computeHelpers()
  }

  /**
   * Computes all the advice in one pass.
   */
  fun compute(filterSpecBuilder: FilterSpecBuilder = FilterSpecBuilder()): ComputedAdvice {
    val unusedComponents = computeUnusedDependencies()
    val unusedDependencies = unusedComponents.mapToSet { it.dependency }

    val undeclaredApiDependencies = computeUndeclaredApiDependencies()
    val undeclaredImplDependencies = computeUndeclaredImplDependencies(undeclaredApiDependencies)

    val changeToApi = computeApiDepsWronglyDeclared()
    val changeToImpl = computeImplDepsWronglyDeclared(unusedDependencies)

    if (dependencyBundles.isNotEmpty()) {
      filterSpecBuilder.dependencyBundleFilter = DependencyBundleFilter(
        bundles = dependencyBundles,
        compileGraph = filterSpecBuilder.compileGraph,
        testCompileGraph = filterSpecBuilder.testCompileGraph,
        transitives = filterSpecBuilder.usedTransitiveComponents
      )
    }

    // update filterSpecBuilder with ktxFilter
    if (ignoreKtx) {
      val ktxFilter = KtxFilter(
        allComponents = allComponents,
        unusedDirectComponents = unusedComponentsWithTransitives,
        usedTransitiveComponents = usedTransitiveComponents,
        unusedDependencies = unusedDependencies
      )
      filterSpecBuilder.addToUniversalFilter(ktxFilter)
    }

    return ComputedAdvice(
      compileOnlyCandidates = compileOnlyCandidates,
      unusedComponents = unusedComponents,
      undeclaredApiDependencies = undeclaredApiDependencies,
      undeclaredImplDependencies = undeclaredImplDependencies,
      changeToApi = changeToApi,
      changeToImpl = changeToImpl,
      unusedProcs = unusedProcs,
      filterSpecBuilder = filterSpecBuilder
    )
  }

  /**
   * Computes all sets of internal helpers in one pass, to avoid having to iterate over the full
   * set of [Component]s more than once.
   *
   * A [Component] is a compileOnly candidate iff:
   * 1. It has already been determined to be based on analysis done in [AnalyzedJar]; OR
   * 2. It is currently on a variant of the `compileOnly` configuration (here we assume users know
   *    what they're doing).
   *
   * A [Component] is a security provider iff:
   * 1. It contains a class that extends the [java.security.Provider] class.
   *
   * A [Component] is a lint-only jar iff:
   * 1. It has already been determined to be based on analysis done in [AnalyzedJar].
   */
  private fun computeHelpers() {
    allComponents.forEach {
      if (it.isCompileOnlyAnnotations || it.dependency.configurationName?.endsWith("compileOnly", ignoreCase = true) == true) {
        (compileOnlyCandidates as MutableSet).add(it)
      } else if (it.securityProviders.isNotEmpty()) {
        (securityProviders as MutableSet).add(it)
      } else if (it.isLintJar) {
        (lintJars as MutableSet).add(it)
      }
    }
  }

  /**
   * A [Dependency] is unused (and should be removed) iff:
   * 1. It is in the set of [unusedComponentsWithTransitives] AND
   * 2. It is not also in the set [compileOnlyCandidates] (we do not suggest removing such
   *    candidates, even if they appear unused) AND
   * 3. It is not also in the set [serviceLoaders] (we cannot safely suggest removing service
   *    loaders, since they are used at runtime) AND
   * 4. It is not also in the set of [securityProviders] (we cannot safely suggest removing security
   *    providers, since they are used at runtime).
   *
   *    --OR--
   *
   * 5. It is not a lint-only jar. (Usage of Android Lint dependencies cannot be detected.)
   */
  private fun computeUnusedDependencies(): Set<ComponentWithTransitives> {
    return unusedComponentsWithTransitives
      .stripCompileOnly()
      .stripServiceLoaders()
      .stripSecurityProviders()
      .stripLintOnlyJars()
      .stripAbiDeps()
  }

  /**
   * A [Dependency] is an undeclared `api` dependency (and should be declared as such) iff:
   * 1. It is part of the project's ABI AND
   * 2. It was not declared (it's [configurationName][Dependency.configurationName] is `null`) AND
   * 3. It was not declared to be `compileOnly` (here we assume users know what they're doing)
   */
  private fun computeUndeclaredApiDependencies(): Set<TransitiveDependency> {
    return abiDeps
      .filterToOrderedSet { it.configurationName == null }
      .stripCompileOnly() // TODO compileOnlyApi
      .mapToSet { it.withParents() }
      .mapToSet { it.withVariants() }
  }

  /**
   * A [Dependency] is an undeclared `implementation` dependency (and should be declared as such) iff:
   * 1. It is in the set of [usedTransitiveComponents] AND
   * 2. It is not an undeclared `api` dependency (see [computeUndeclaredApiDependencies]) AND
   * 3. It is not a `compileOnly` candidate (see [computeHelpers])
   */
  private fun computeUndeclaredImplDependencies(
    undeclaredApiDeps: Set<TransitiveDependency>
  ): Set<TransitiveDependency> {
    return usedTransitiveComponents
      .mapToOrderedSet { it.dependency }
      .stripCompileOnly()
      .mapToSet { it.withParents() }
      // Exclude any transitives which will be api dependencies
      .filterNoneMatchingSorted(undeclaredApiDeps)
      .mapToSet { it.withVariants() }
  }

  /**
   * A [Dependency] is a "wrongly declared" api dep (and should be changed) iff:
   * 1. It is not transitive ([configuration][Dependency.configurationName] must be non-null); AND
   * 2. It _should_ be on `api`, but is on something else; AND
   * 3. It is not a `compileOnly` candidate (see [computeHelpers]); AND
   * 4. It is not already on a variant of `api`.
   */
  private fun computeApiDepsWronglyDeclared(): Set<VariantDependency> {
    return abiDeps
      // Filter out those with a null configuration, as they are handled elsewhere
      .filterToOrderedSet { it.configurationName != null }
      .stripCompileOnly() // TODO compileOnlyApi
      // Filter out those on some variant of `api`
      .stripVariantsOf("api")
      .mapToOrderedSet { it.withVariants() }
  }

  /**
   * A [Dependency] is a "wrongly declared" impl dep (and should be changed) iff:
   * 1. It is not transitive ([configuration][Dependency.configurationName] must be non-null); AND
   * 2. It is used; AND
   * 3. It is not part of the project's ABI; AND
   * 4. It is not a `compileOnly` candidate (see [computeHelpers]); AND
   * 5. It is not already on a variant of `implementation`.
   */
  private fun computeImplDepsWronglyDeclared(
    unusedDependencies: Set<Dependency>
  ): Set<VariantDependency> {
    return allDeclaredDeps
      // Filter out those with a null configuration, as they are handled elsewhere
      .filterToOrderedSet { it.configurationName != null }
      // Filter out those on some variant of `implementation` (assume the variant is correct)
      .stripVariantsOf("implementation")
      // Filter out those that are unused
      .filterNoneMatchingSorted(unusedDependencies)
      // Filter out those that actually should be api
      .filterNoneMatchingSorted(abiDeps)
      .stripCompileOnly()
      .stripRuntimeOnly()
      .mapToOrderedSet { it.withVariants() }
  }

  private fun <T : HasDependency> Iterable<T>.stripVariantsOf(confName: String): Set<T> =
    filterToOrderedSet {
      it.dependency.configurationName?.endsWith(confName.capitalizeSafely()) == false
    }

  private fun Dependency.withParents(): TransitiveDependency {
    val parents = mutableSetOf<Dependency>()
    allComponentsWithTransitives.forEach { component ->
      if (component.usedTransitiveDependencies.orEmpty().any { it == this }) {
        parents.add(component.dependency)
      }
    }
    return TransitiveDependency(this, parents)
  }

  private fun TransitiveDependency.withVariants(): TransitiveDependency {
    return usedTransitiveComponents.find { it.dependency == dependency }?.let {
      copy(variants = it.variants)
    } ?: this
  }

  private fun Dependency.withVariants(): VariantDependency {
    val variants = usedVariantDependencies.find { it.dependency == this }?.variants.orEmpty()
    return VariantDependency(this, variants)
  }

  private fun <T : HasDependency> Iterable<T>.stripCompileOnly(): Set<T> {
    return filterToOrderedSet { container ->
      compileOnlyCandidates.none { compileOnly ->
        container.dependency == compileOnly.dependency
      }
    }
  }

  private fun <T : HasDependency> Iterable<T>.stripRuntimeOnly(): Set<T> {
    return filterNotToOrderedSet {
      it.dependency.configurationName?.endsWith("runtimeOnly", ignoreCase = true) == true
    }
  }

  private fun <T : HasDependency> Iterable<T>.stripServiceLoaders(): Set<T> {
    return filterToOrderedSet { container ->
      serviceLoaders.none { serviceLoader ->
        container.dependency == serviceLoader.dependency
      }
    }
  }

  private fun <T : HasDependency> Iterable<T>.stripSecurityProviders(): Set<T> {
    return filterToOrderedSet { container ->
      securityProviders.none { securityProvider ->
        container.dependency == securityProvider.dependency
      }
    }
  }

  private fun <T : HasDependency> Iterable<T>.stripLintOnlyJars(): Set<T> {
    return filterToOrderedSet { container ->
      lintJars.none { lintJar ->
        container.dependency == lintJar.dependency
      }
    }
  }

  private fun <T : HasDependency> Iterable<T>.stripAbiDeps(): Set<T> {
    return filterToOrderedSet { container ->
      abiDeps.none { abiDep ->
        container.dependency == abiDep.dependency
      }
    }
  }
}
