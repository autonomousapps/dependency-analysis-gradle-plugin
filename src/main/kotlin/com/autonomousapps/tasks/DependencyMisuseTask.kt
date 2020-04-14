@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
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

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val declaredDependencies: RegularFileProperty

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val usedClasses: RegularFileProperty

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val usedInlineDependencies: RegularFileProperty

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val usedConstantDependencies: RegularFileProperty

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:Optional
  @get:InputFile
  abstract val manifests: RegularFileProperty

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:Optional
  @get:InputFile
  abstract val usedAndroidResBySourceDependencies: RegularFileProperty

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:Optional
  @get:InputFile
  abstract val usedAndroidResByResDependencies: RegularFileProperty

  @get:OutputFile
  abstract val outputUnusedDependencies: RegularFileProperty

  @get:OutputFile
  abstract val outputUsedTransitives: RegularFileProperty

  @TaskAction
  fun action() {
    // Input
    val declaredDependenciesFile = declaredDependencies.get().asFile
    val usedClassesFile = usedClasses.get().asFile
    val usedInlineDependenciesFile = usedInlineDependencies.get().asFile
    val usedConstantDependenciesFile = usedConstantDependencies.get().asFile
    val manifestsFile = manifests.orNull?.asFile
    val usedAndroidResBySourceFile = usedAndroidResBySourceDependencies.orNull?.asFile
    val usedAndroidResByResFile = usedAndroidResByResDependencies.orNull?.asFile
    val resolvedComponentResult: ResolvedComponentResult = runtimeConfiguration
      .incoming
      .resolutionResult
      .root

    // Output
    val outputUnusedDependenciesFile = outputUnusedDependencies.get().asFile
    val outputUsedTransitivesFile = outputUsedTransitives.get().asFile

    // Cleanup prior execution
    outputUnusedDependenciesFile.delete()
    outputUsedTransitivesFile.delete()

    val detector = MisusedDependencyDetector(
      declaredComponents = declaredDependenciesFile.readText().fromJsonList(),
      usedClasses = usedClassesFile.readLines(),
      usedInlineDependencies = usedInlineDependenciesFile.readText().fromJsonList(),
      usedConstantDependencies = usedConstantDependenciesFile.readText().fromJsonList(),
      manifests = manifestsFile?.readText()?.fromJsonList(),
      usedAndroidResBySourceDependencies = usedAndroidResBySourceFile?.readText()?.fromJsonList(),
      usedAndroidResByResDependencies = usedAndroidResByResFile?.readText()?.fromJsonList(),
      root = resolvedComponentResult
    )
    val dependencyReport = detector.detect()

    // Reports
    outputUnusedDependenciesFile.writeText(dependencyReport.unusedDepsWithTransitives.toJson())
    outputUsedTransitivesFile.writeText(dependencyReport.usedTransitives.toJson())
  }
}

internal class MisusedDependencyDetector(
  private val declaredComponents: List<Component>,
  private val usedClasses: List<String>,
  private val usedInlineDependencies: List<Dependency>,
  private val usedConstantDependencies: List<Dependency>,
  private val manifests: List<Manifest>?,
  private val usedAndroidResBySourceDependencies: List<Dependency>?,
  private val usedAndroidResByResDependencies: List<AndroidPublicRes>?,
  private val root: ResolvedComponentResult
) {
  fun detect(): DependencyReport {
    val unusedDeps = mutableListOf<Dependency>()
    val usedTransitiveComponents = mutableSetOf<TransitiveComponent>()
    val usedDirectClasses = mutableSetOf<String>()

    declaredComponents
      // Exclude dependencies with zero class files (such as androidx.legacy:legacy-support-v4)
      .filterNot { it.classes.isEmpty() }
      .forEach { component ->
        var count = 0
        val classes = sortedSetOf<String>()

        component.classes.forEach { declClass ->
          // Looking for unused direct dependencies
          if (!component.isTransitive) {
            if (!usedClasses.contains(declClass)) {
              // Unused class
              count++
            } else {
              // Used class
              usedDirectClasses.add(declClass)
            }
          }

          // Looking for used transitive dependencies
          if (component.isTransitive
            // Assume all these come from android.jar
            && !declClass.startsWith("android.")
            && usedClasses.contains(declClass)
            // Not in the set of used direct dependencies
            && !usedDirectClasses.contains(declClass)
          ) {
            classes.add(declClass)
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
          // Exclude modules that appear in the manifest (e.g., they supply Android components like
          // ContentProviders)
          && component.hasNoManifestMatches()
        ) {
          unusedDeps.add(component.dependency)
        }

        if (classes.isNotEmpty()) {
          usedTransitiveComponents.add(TransitiveComponent(component.dependency, classes))
        }
      }

    // Connect used-transitives to direct dependencies
    val unusedDepsWithTransitives: Set<UnusedDirectComponent> =
      unusedDeps.mapNotNullToSet { unusedDep ->
        unusedDep.asResolvedDependencyResult()?.let { rdr ->
          relate(rdr, UnusedDirectComponent(unusedDep, mutableSetOf()), usedTransitiveComponents)
        }
      }

    return DependencyReport(unusedDepsWithTransitives, usedTransitiveComponents)
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

  /**
   * If the component's dependency matches any of our manifest dependencies, and that manifest provides an Android
   * component, then it is used.
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
   * one place, [KtxFilter.computeKtxTransitives][com.autonomousapps.internal.advice.KtxFilter.computeKtxTransitives]
   *
   * - [unusedDependency] contains information about the dependency graph rooted on the unused
   *   dependency, and then on each node below it as the algorithm traverses the graph recursively.
   * - [unusedDirectComponent] is the "UnusedDirectComponent", which we are currently building with
   *   the set of transitive dependencies it brings along and that are used. It represents the same
   *   "thing" as `unusedDependency`, but with additional information included in its model.
   * - [usedTransitiveComponents] is the set of transitively-declared components that are used
   *   directly by the project.
   */
  private fun relate(
    unusedDependency: ResolvedDependencyResult,
    unusedDirectComponent: UnusedDirectComponent,
    usedTransitiveComponents: MutableSet<TransitiveComponent>
  ): UnusedDirectComponent {
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

        if (usedTransitiveComponents.contains(transitiveIdentifier)) {
          unusedDirectComponent.usedTransitiveDependencies.add(Dependency(
            identifier = transitiveIdentifier,
            resolvedVersion = transitiveResolvedVersion
          ))
        }
        relate(transitiveNode, unusedDirectComponent, usedTransitiveComponents)
      }
    return unusedDirectComponent
  }

  private fun Set<TransitiveComponent>.contains(identifier: String): Boolean {
    return map { trans -> trans.dependency.identifier }.contains(identifier)
  }

  internal class DependencyReport(
    val unusedDepsWithTransitives: Set<UnusedDirectComponent>,
    val usedTransitives: Set<TransitiveComponent>
  )
}
