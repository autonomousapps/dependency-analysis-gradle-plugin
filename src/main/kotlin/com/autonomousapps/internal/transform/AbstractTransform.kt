// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.transform

import com.autonomousapps.extension.DependenciesHandler
import com.autonomousapps.graph.Graphs.children
import com.autonomousapps.graph.Graphs.root
import com.autonomousapps.internal.DependencyScope
import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.internal.utils.mapToSet
import com.autonomousapps.internal.utils.newSetMultimap
import com.autonomousapps.model.*
import com.autonomousapps.model.Coordinates.Companion.copy
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.internal.ProjectType
import com.autonomousapps.model.internal.declaration.Bucket
import com.autonomousapps.model.internal.declaration.ConfigurationNames
import com.autonomousapps.model.internal.declaration.Declaration
import com.autonomousapps.model.internal.intermediates.Usage
import com.autonomousapps.model.source.SourceKind
import com.google.common.collect.SetMultimap
import org.gradle.api.attributes.Category

@Suppress("UnstableApiUsage")
internal abstract class AbstractTransform(
  protected val coordinates: Coordinates,
  protected val declarations: Set<Declaration>,
  protected val explicitSourceSets: Set<String>,
  protected val configurationNames: ConfigurationNames,
  protected val buildPath: String,
  private val dependencyGraph: Map<String, DependencyGraphView>,
  private val isKaptApplied: Boolean,
) : Usage.Transform {

  protected val mapper = UsageToConfigurationMapper(
    isKaptApplied = isKaptApplied,
    projectType = ProjectType.ANDROID,
  )

  /**
   * Returns the set of direct (non-transitive) dependencies from [dependencyGraph], associated with the source sets
   * ([Variant.variant][SourceKind]) they're related to.
   *
   * These are _direct_ dependencies that are not _declared_ because they're coming from associated classpaths. For
   * example, the `test` source set extends from the `main` source set (and also the compile and runtime classpaths).
   */
  protected val directDependencies: SetMultimap<String, SourceKind> by unsafeLazy {
    newSetMultimap<String, SourceKind>().apply {
      dependencyGraph.values.forEach { graphView ->
        val root = graphView.graph.root()
        graphView.graph.children(root).forEach { directDependency ->
          val identifier = directDependency.normalizedIdentifier(buildPath)
          put(identifier, graphView.sourceKind)
        }
      }
    }
  }

  /**
   * This results in a map like:
   * * "group:name:1.0" -> (compileClasspath, runtimeClasspath)
   * * ":project" -> (compileClasspath)
   *
   * etc.
   */
  protected val dependenciesToClasspaths: SetMultimap<String, String> by unsafeLazy {
    newSetMultimap<String, String>().apply {
      dependencyGraph.values.forEach { graphView ->
        graphView.graph.nodes().forEach { node ->
          val identifier = node.normalizedIdentifier(buildPath)
          put(identifier, graphView.configurationName)
        }
      }
    }
  }

  /** Use coordinates/variant of the original declaration when reporting remove/change as it is more precise. */
  protected fun declarationCoordinates(decl: Declaration): Coordinates {
    // NB: written without `when` guards so it compiles at language version 2.1 (Gradle 8.x compat; see issue 1671).
    return when (coordinates) {
      is IncludedBuildCoordinates -> {
        if (decl.identifier.startsWith(":")) coordinates.resolvedProject else coordinates
      }

      // This handles the case where we have an unused dependency because it's been excluded via
      // configurations.<foo>.exclude(group = "group", module = "module")
      is FlatCoordinates -> {
        if (decl.version != null) {
          ModuleCoordinates(coordinates.identifier, decl.version, decl.gradleVariantIdentification)
        } else {
          coordinates
        }
      }

      else -> coordinates
    }.copy(decl.identifier, decl.gradleVariantIdentification)
  }

  protected fun Set<Declaration>.forCoordinates(coordinates: Coordinates): Set<Declaration> {
    return asSequence()
      .filter { declaration ->
        declaration.identifier == coordinates.identifier
          // In the special case of IncludedBuildCoordinates, the declaration might be a 'project(...)' dependency
          // if subprojects inside an included build depend on each other.
          || (coordinates is IncludedBuildCoordinates) && declaration.identifier == coordinates.resolvedProject.identifier
      }
      .filter { it.isJarDependency() && it.gradleVariantIdentification.variantMatches(coordinates) }
      .toSet()
  }

  protected fun isSingleBucketForSingleVariant(usages: Set<Usage>): Boolean {
    return if (usages.size == 1) {
      true
    } else {
      usages.mapToSet { it.bucket }.size == 1 && usages.mapToSet { it.sourceKind.base() }.size == 1
    }
  }

  protected fun Sequence<Usage>.filterUsed() = filterNot { it.bucket == Bucket.NONE }

  /**
   * Does the dependency point to one (or multiple) Jars, or is it just Metadata (i.e. a platform)
   * that we always want to keep?
   */
  protected fun Declaration.isJarDependency() =
    gradleVariantIdentification.attributes[Category.CATEGORY_ATTRIBUTE.name].let {
      it != Category.REGULAR_PLATFORM && it != Category.ENFORCED_PLATFORM
    }

  protected fun Declaration.findSourceKind(hasCustomSourceSets: Boolean): SourceKind? {
    return sourceSetKind(hasCustomSourceSets, configurationNames)
  }

  protected fun hasCustomSourceSets(usages: Set<Usage>): Boolean {
    return usages.any { it.sourceKind.kind == SourceKind.CUSTOM_JVM_KIND }
  }

  /**
   * Returns true if [sourceSet] is in the set of [explicitSourceSets], or if [explicitSourceSets] is set for all source
   * sets.
   */
  protected fun explicitFor(sourceSet: String?): Boolean {
    return sourceSet in explicitSourceSets
      || DependenciesHandler.isExplicitForAll(explicitSourceSets)
  }

  protected fun addRemainingUsages(usages: Set<Usage>, advice: MutableSet<Advice>) {
    usages.asSequence()
      // Don't add unused usages!
      .filterUsed()
      // Don't add runtimeOnly or compileOnly (compileOnly, compileOnlyApi, providedCompile) declarations
      // nb: this probably remains the correct choice, but it can lead to issues when we remove an "unused" dependency
      // and fail to add a required runtimeOnly dependency that was part of the unused dep's transitive graph. Removing
      // this line would lead to "super strict" declarations that are, perhaps, more bazel-like (every classpath
      // consists only of positively-declared dependencies).
      .filterNot { usage -> usage.bucket == Bucket.COMPILE_ONLY }
      // Don't add something that is only present on the compileClasspath as that will change the runtimeClasspath,
      // which we do not want to do.
      .filterNot { usage ->
        val identifier = if (coordinates is IncludedBuildCoordinates) {
          coordinates.resolvedProject.identifier
        } else {
          coordinates.identifier
        }

        val currentClasspaths = dependenciesToClasspaths.get(identifier)

        // if it is a runtime usage and this dep isn't currently in the matching runtime classpath, don't add it there.
        usage.isRuntimeUsage() && !usage.runtimeMatches(currentClasspaths)
      }
      .mapTo(advice) { usage ->
        val preferredCoordinatesNotation =
          if (coordinates is IncludedBuildCoordinates && coordinates.resolvedProject.buildPath == buildPath) {
            coordinates.resolvedProject
          } else {
            coordinates
          }
        Advice.ofAdd(preferredCoordinatesNotation.withoutDefaultCapability(), mapper.toConfiguration(usage))
      }
  }

  protected fun removeRemainingDeclarations(declarations: MutableSet<Declaration>, advice: MutableSet<Advice>) {
    declarations.asSequence()
      // Don't remove runtimeOnly or compileOnly declarations
      .filterNot { decl ->
        decl.bucket(configurationNames) == Bucket.COMPILE_ONLY || decl.bucket(configurationNames) == Bucket.RUNTIME_ONLY
      }
      .mapTo(advice) { declaration ->
        Advice.ofRemove(declarationCoordinates(declaration), declaration)
      }
  }

  /**
   * We don't want to be forced to redeclare dependencies in related source sets. Consider (pseudocode):
   * ```
   * // build.gradle
   * sourceSets.functionalTest.extendsFrom sourceSets.test
   *
   * dependencies {
   *   testImplementation 'foo:bar:1.0'
   *   // functionalTestImplementation will also "inherit" the 'foo:bar:1.0' dependency.
   * }
   * ```
   *
   * nb: returning false means "keep this advice."
   */
  protected fun isDeclaredInRelatedSourceSet(allAdvice: Set<Advice>, advice: Advice): Boolean {
    if (!advice.isAnyAdd()) return false

    val sourceSetName = DependencyScope.sourceSetName(advice.toConfiguration!!)

    // With explicit source sets, a source set may not be related to any other.
    if (explicitFor(sourceSetName)) return false

    val isTestRelated = sourceSetName?.let { DependencyScope.isTestRelated(it) } == true

    // Don't strip advice that improves correctness (e.g., declaring something on an "api-like" configuration).
    // Unless it's api-like on a test source set, which makes no sense.
    if (advice.isToApiLike() && !isTestRelated) return false

    // Instead of attempting a complex algorithm to get it "just right", if we see ANY downgrade advice, we bail out.
    // This particular function is already an optimization to support a scenario we arguably shouldn't, leading to
    // increasingly complex and brittle code. That is, it supports the special case that main deps are generally
    // visible to test. Perhaps we should just stop doing that.
    val anyDowngrade = allAdvice.any { it.isDowngrade() }
    if (anyDowngrade) return false

    val sourceSets = directDependencies[advice.coordinates.identifier].map { it.name }

    // There's "no point" in adding a new declaration when that dependency is already available as a direct dependency,
    // UNLESS we also happen to have some advice that might DOWNGRADE/REMOVE that declaration. For example, we might be
    // about to advise the user to remove an `implementation` dependency, in which case the advice to also add a
    // `testImplementation` dependency IS NOT redundant and SHOULD NOT be filtered out.
    return sourceSetName in sourceSets
  }

  /**
   * If we're adding an api-like declaration to a test-like configuration, instead suggest adding it to an
   * implementation-like configuration. Tests don't have APIs.
   */
  protected fun downgradeTestDependencies(advice: Advice): Advice {
    if (!advice.isAnyAdd()) return advice
    if (!advice.isToApiLike()) return advice

    val sourceSetName = DependencyScope.sourceSetName(advice.toConfiguration!!) ?: return advice
    if (!DependencyScope.isTestRelated(sourceSetName)) return advice

    return advice.copy(toConfiguration = "${sourceSetName}Implementation")
  }
}
