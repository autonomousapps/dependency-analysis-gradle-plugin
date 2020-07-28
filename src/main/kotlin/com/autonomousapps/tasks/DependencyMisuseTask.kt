@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.*
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

/**
 * Produces a report of unused direct dependencies and used transitive dependencies.
 */
@CacheableTask
abstract class DependencyMisuseTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces a report of unused direct dependencies and used transitive dependencies"
  }

  /**
   * This is the "official" input for wiring task dependencies correctly, but is otherwise
   * unused.
   */
  @get:Classpath
  lateinit var artifactFiles: FileCollection

  /**
   * This is what the task actually uses as its input.
   */
  @get:Internal
  lateinit var runtimeConfiguration: Configuration

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val declaredDependencies: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val usedClasses: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val usedInlineDependencies: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val usedConstantDependencies: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val usedGenerally: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:Optional
  @get:InputFile
  abstract val manifests: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:Optional
  @get:InputFile
  abstract val usedAndroidResBySourceDependencies: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:Optional
  @get:InputFile
  abstract val usedAndroidResByResDependencies: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:Optional
  @get:InputFile
  abstract val proguardClasses: RegularFileProperty

  @get:OutputFile
  abstract val outputAllComponents: RegularFileProperty

  @get:OutputFile
  abstract val outputUnusedComponents: RegularFileProperty

  @get:OutputFile
  abstract val outputUsedTransitives: RegularFileProperty

  @get:OutputFile
  abstract val outputUsedVariantDependencies: RegularFileProperty

  @TaskAction
  fun action() {
    // Outputs
    val outputAllComponentsFile = outputAllComponents.getAndDelete()
    val outputUnusedComponentsFile = outputUnusedComponents.getAndDelete()
    val outputUsedTransitivesFile = outputUsedTransitives.getAndDelete()
    val outputUsedVariantDependenciesFile = outputUsedVariantDependencies.getAndDelete()

    // Inputs
    val resolvedComponentResult: ResolvedComponentResult = runtimeConfiguration
      .incoming
      .resolutionResult
      .root

    val dependencyReport = MisusedDependencyDetector(
      declaredComponents = declaredDependencies.fromJsonSet(),
      usedClasses = usedClasses.fromJsonSet(),
      usedInlineDependencies = usedInlineDependencies.fromJsonSet(),
      usedConstantDependencies = usedConstantDependencies.fromJsonSet(),
      usedGenerally = usedGenerally.fromJsonSet(),
      manifests = manifests.fromNullableJsonSet(),
      usedAndroidResBySourceDependencies = usedAndroidResBySourceDependencies.fromNullableJsonSet(),
      usedAndroidResByResDependencies = usedAndroidResByResDependencies.fromNullableJsonSet(),
      proguardClasses = proguardClasses.fromNullableJsonSet(),
      root = resolvedComponentResult
    ).detect()

    // Reports
    outputAllComponentsFile.writeText(dependencyReport.allComponentsWithTransitives.toJson())
    outputUnusedComponentsFile.writeText(dependencyReport.unusedComponentsWithTransitives.toJson())
    outputUsedTransitivesFile.writeText(dependencyReport.usedTransitives.toJson())
    outputUsedVariantDependenciesFile.writeText(dependencyReport.usedDependencies.toJson())
  }
}

internal class MisusedDependencyDetector(
  private val declaredComponents: Set<Component>,
  private val usedClasses: Set<VariantClass>,
  private val usedInlineDependencies: Set<Dependency>,
  private val usedConstantDependencies: Set<Dependency>,
  private val usedGenerally: Set<Dependency>,
  private val manifests: Set<Manifest>?,
  private val usedAndroidResBySourceDependencies: Set<Dependency>?,
  private val usedAndroidResByResDependencies: Set<AndroidPublicRes>?,
  private val proguardClasses: Set<ProguardClasses>?,
  private val root: ResolvedComponentResult
) {
  fun detect(): DependencyReport {
    val unusedDeps = mutableListOf<Dependency>()
    val usedTransitiveComponents = mutableSetOf<TransitiveComponent>()
    val usedDirectClasses = mutableSetOf<String>()
    val usedDependencies = mutableMapOf<Dependency, MutableSet<String>>()

    declaredComponents
      // Exclude dependencies with zero class files (such as androidx.legacy:legacy-support-v4)
      .filterNot { it.classes.isEmpty() }
      .forEach { component ->
        var count = 0
        val variantClasses = sortedSetOf<VariantClass>()

        component.classes.forEach { declClass ->
          // Find the "variant-aware" class
          val variantClass = usedClasses.find { it.theClass == declClass }

          // Looking for unused direct dependencies
          if (!component.isTransitive) {
            if (variantClass == null) {
              // Unused class
              count++
            } else {
              // Used class
              usedDirectClasses.add(declClass)
              usedDependencies.merge(component.dependency, variantClass.variants.toMutableSet()) { oldSet, newSet ->
                oldSet.apply { addAll(newSet) }
              }
            }
          }

          // Looking for used transitive dependencies
          if (component.isTransitive
            // Assume all these come from android.jar
            && !declClass.startsWith("android.")
            && variantClass != null
            // Not in the set of used direct dependencies
            && !usedDirectClasses.contains(declClass)
          ) {
            variantClasses.add(variantClass)
          }
        }

        if (count == component.classes.size
          // Exclude modules that have inline usages
          && component.hasNoInlineUsages()
          // Exclude modules that have Android res (by source) usages
          && component.hasNoAndroidResBySourceUsages()
          // Exclude modules that have Android res (by res) usages
          && component.hasNoAndroidResByResUsages()
          // Exclude modules that have constant usages
          && component.hasNoConstantUsages()
          // Exclude modules that have types used in a general context
          && component.hasNoGeneralUsages()
          // Exclude modules that appear in the manifest (e.g., they supply Android components like
          // ContentProviders)
          && component.hasNoManifestMatches()
          // Exclude modules that contain proguard rules referencing a class
          && component.hasNoProguardUsages()
        ) {
          unusedDeps.add(component.dependency)
        }

        if (variantClasses.isNotEmpty()) {
          val classes = variantClasses.mapToOrderedSet { it.theClass }
          val variants = variantClasses.flatMapToOrderedSet { it.variants }
          usedTransitiveComponents.add(TransitiveComponent(
            dependency = component.dependency,
            usedTransitiveClasses = classes,
            variants = variants
          ))
        }
      }

    // Connect used-transitives to direct dependencies
    val allComponentsWithTransitives: Set<ComponentWithTransitives> =
      declaredComponents.mapToSet { it.dependency }.mapNotNullToSet { dep ->
        dep.asResolvedDependencyResult()?.let { rdr ->
          relate(
            unusedDependency = rdr,
            unusedDirectComponent = ComponentWithTransitives(dep, mutableSetOf()),
            usedTransitiveComponents = usedTransitiveComponents
          )
        }
      }

    // Filter above to only get those that are unused
    val unusedDepsWithTransitives: Set<ComponentWithTransitives> = allComponentsWithTransitives
      .filterToSet { comp ->
        unusedDeps.any { it == comp.dependency }
      }

    // Performance diagnostics
    //println("Counts:\n" + counter.entries.joinToString(separator = "\n") { "${it.key}: ${it.value}" })

    return DependencyReport(
      allComponentsWithTransitives = allComponentsWithTransitives,
      unusedComponentsWithTransitives = unusedDepsWithTransitives,
      usedTransitives = usedTransitiveComponents,
      usedDependencies = usedDependencies.toVariantDependencies()
    )
  }

  private fun Map<Dependency, Set<String>>.toVariantDependencies(): Set<VariantDependency> {
    val set = mutableSetOf<VariantDependency>()
    forEach { (dep, variants) ->
      set.add(VariantDependency(dep, variants))
    }
    return set
  }

  private fun Component.hasNoInlineUsages(): Boolean {
    return usedInlineDependencies.none { it == dependency }
  }

  private fun Component.hasNoAndroidResBySourceUsages(): Boolean {
    return usedAndroidResBySourceDependencies?.none { it == dependency } ?: true
  }

  private fun Component.hasNoAndroidResByResUsages(): Boolean {
    return usedAndroidResByResDependencies?.none { it.dependency == dependency } ?: true
  }

  private fun Component.hasNoConstantUsages(): Boolean {
    return usedConstantDependencies.none { it == dependency }
  }

  private fun Component.hasNoGeneralUsages(): Boolean {
    return usedGenerally.none { it == dependency }
  }

  /**
   * If the component's dependency matches any of our proguard-rules-supplying dependencies, and
   * that dependency supplies any rule directly referencing a class, then it is used.
   */
  private fun Component.hasNoProguardUsages(): Boolean {
    val proguard = proguardClasses?.find { it.dependency == dependency } ?: return true
    return proguard.classes.isEmpty()
  }

  /**
   * If the component's dependency matches any of our manifest dependencies, and that manifest
   * provides an Android component, then it is used.
   */
  private fun Component.hasNoManifestMatches(): Boolean {
    val manifest = manifests?.find { it.dependency == dependency } ?: return true
    return !manifest.hasComponents
  }

  private fun Dependency.asResolvedDependencyResult(): ResolvedDependencyResult? =
    root.dependencies.filterIsInstance<ResolvedDependencyResult>().find { rdr ->
      identifier == rdr.selected.id.asString()
    }

  /**
   * This algorithm "relates" direct components (those declared in the build script) with the
   * used transitive components it may contribute (to n degrees) to the graph. Multiple components
   * may contribute the same transitive dependency. The algorithm ensures this many-to-many
   * relationship is discovered.
   *
   * This algorithm must traverse the full graph because the following is possible:
   *
   * ```
   *     a -> b -> ... -> n
   * ```
   * where `a` is directly declared but is unused, `a` declares `b`, and `b` declares ..., which
   * eventually declares `n`. `n` is used by the project (and is not declared). The algorithm will
   * relate `a` to `n` so we know the many provenances of `n`. This knowledge is used in at least
   * one place, [KtxFilter.computeKtxTransitives][com.autonomousapps.internal.advice.filter.KtxFilter.computeKtxTransitives]
   *
   * - [unusedDependency] contains information about the dependency graph rooted on the unused
   *   dependency, and then on each node below it as the algorithm traverses the graph recursively.
   * - [unusedDirectComponent] is the "UnusedDirectComponent", which we are currently building with
   *   the set of transitive dependencies it brings along and that are used. It represents the same
   *   "thing" as `unusedDependency`, but with additional information included in its model.
   * - [usedTransitiveComponents] is the set of transitively-declared components that are used
   *   directly by the project.
   * - [visitedNodes] is the set of nodes in the graph that have already been visited _for the
   *   `unusedDependency`_ node. This is a massive performance optimization, eliding 99% of the
   *   potential work in one test.
   */
  private fun relate(
    unusedDependency: ResolvedDependencyResult,
    unusedDirectComponent: ComponentWithTransitives,
    usedTransitiveComponents: Set<TransitiveComponent>,
    visitedNodes: MutableSet<String> = mutableSetOf()
  ): ComponentWithTransitives {
    unusedDependency
      // the dependency actually selected by dependency resolution
      .selected
      // the dependencies of the selected dependency
      .dependencies
      // only those that have been fully resolved
      .filterIsInstance<ResolvedDependencyResult>()
      .forEach { transitiveNode ->
        val transitiveIdentifier = transitiveNode.selected.id.asString()
        val transitiveResolvedVersion = transitiveNode.selected.id.resolvedVersion()

        if (!visitedNodes.contains(transitiveIdentifier)) {
          // Performance diagnostics
          //counter.merge(transitiveIdentifier, 1) { oldValue, increment -> oldValue + increment }

          if (usedTransitiveComponents.contains(transitiveIdentifier)) {
            unusedDirectComponent.usedTransitiveDependencies.add(Dependency(
              identifier = transitiveIdentifier,
              resolvedVersion = transitiveResolvedVersion
            ))
          }
          relate(
            unusedDependency = transitiveNode,
            unusedDirectComponent = unusedDirectComponent,
            usedTransitiveComponents = usedTransitiveComponents,
            visitedNodes = visitedNodes.apply { add(transitiveIdentifier) }
          )
        }
      }
    return unusedDirectComponent
  }

  // Performance diagnostics
  //private val counter = mutableMapOf<String, Int>()

  private fun Set<TransitiveComponent>.contains(identifier: String): Boolean {
    return map { trans -> trans.dependency.identifier }.contains(identifier)
  }

  internal class DependencyReport(
    val allComponentsWithTransitives: Set<ComponentWithTransitives>,
    val unusedComponentsWithTransitives: Set<ComponentWithTransitives>,
    val usedTransitives: Set<TransitiveComponent>,
    val usedDependencies: Set<VariantDependency>
  )
}
