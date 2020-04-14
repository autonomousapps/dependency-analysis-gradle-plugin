@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.AndroidPublicRes
import com.autonomousapps.internal.Component
import com.autonomousapps.internal.Dependency
import com.autonomousapps.internal.Manifest
import com.autonomousapps.internal.TransitiveComponent
import com.autonomousapps.internal.UnusedDirectComponent
import com.autonomousapps.internal.utils.asString
import com.autonomousapps.internal.utils.filterToSet
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.mapNotNullToSet
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.internal.utils.resolvedVersion
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

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

    logger.debug(
      """
            |Unused dependencies report:          ${outputUnusedDependenciesFile.path}
            |Used-transitive dependencies report: ${outputUsedTransitivesFile.path}
            |
            |Completely unused dependencies:
            |${if (dependencyReport.completelyUnusedDeps.isEmpty()) "none" else dependencyReport.completelyUnusedDeps.joinToString(
        separator = "\n- ",
        prefix = "- "
      )}
        """.trimMargin()
    )
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
    val unusedLibs = mutableListOf<Dependency>()
    val usedTransitives = mutableSetOf<TransitiveComponent>()
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
          // Exclude modules that appear in the manifest (e.g., they supply Android components like ContentProviders)
          && component.hasNoManifestMatches()
        ) {
          unusedLibs.add(component.dependency)
        }

        if (classes.isNotEmpty()) {
          usedTransitives.add(TransitiveComponent(component.dependency, classes))
        }
      }

    // Connect used-transitives to direct dependencies
    val unusedDepsWithTransitives = unusedLibs.mapNotNullToSet { unusedLib ->
      root.dependencies.filterIsInstance<ResolvedDependencyResult>().find {
        unusedLib.identifier == it.selected.id.asString()
      }?.let {
        relate(it, UnusedDirectComponent(unusedLib, mutableSetOf()), usedTransitives)
      }
    }

    // This is for printing to the console. A simplified view
    val completelyUnusedDeps = unusedDepsWithTransitives
      .filterToSet { it.usedTransitiveDependencies.isEmpty() }
      .mapToOrderedSet { it.dependency.identifier }

    return DependencyReport(
      unusedDepsWithTransitives,
      usedTransitives,
      completelyUnusedDeps
    )
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

  /**
   * This recursive function maps used-transitives (undeclared dependencies, nevertheless used
   * directly) to direct dependencies (those actually declared "directly" in the build script).
   */
  private fun relate(
    resolvedDependency: ResolvedDependencyResult,
    unusedDep: UnusedDirectComponent,
    transitives: MutableSet<TransitiveComponent>
  ): UnusedDirectComponent {
    resolvedDependency
      // the dependency actually selected by dependency resolution
      .selected
      // the dependencies of the selected dependency
      .dependencies
      // only those that have been fully resolved
      .filterIsInstance<ResolvedDependencyResult>()
      .forEach {
        val identifier = it.selected.id.asString()
        val resolvedVersion = it.selected.id.resolvedVersion()

        if (transitives.map { trans -> trans.dependency.identifier }.contains(identifier)) {
          unusedDep.usedTransitiveDependencies.add(Dependency(identifier, resolvedVersion))
        }
        relate(it, unusedDep, transitives)
      }
    return unusedDep
  }

  internal class DependencyReport(
    val unusedDepsWithTransitives: Set<UnusedDirectComponent>,
    val usedTransitives: Set<TransitiveComponent>,
    val completelyUnusedDeps: Set<String>
  )
}
