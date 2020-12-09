@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.*
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

/**
 * Produces a report of unused direct dependencies and used transitive dependencies.
 */
@CacheableTask
abstract class DependencyMisuseTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report of unused direct dependencies and used transitive dependencies"
  }

  /**
   * This is the "official" input for wiring task dependencies correctly, but is otherwise unused.
   */
  @get:Classpath
  lateinit var artifactFiles: FileCollection

  /**
   * This is what the task actually uses as its input.
   */
  @get:Internal
  lateinit var compileConfiguration: Configuration

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
  abstract val nativeLibDependencies: RegularFileProperty

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
    val resolvedComponentResult: ResolvedComponentResult = compileConfiguration
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
      nativeLibDependencies = nativeLibDependencies.fromNullableJsonSet(),
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
  private val nativeLibDependencies: Set<NativeLibDependency>?,
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
          // Exclude modules that have bundled native libs (.so files)
          && component.hasNoNativeLibUsages()
          // Exclude modules that have constant usages
          && component.hasNoConstantUsages()
          // Exclude modules that have types used in a general context
          && component.hasNoGeneralUsages()
          // Exclude modules that appear in the manifest (e.g., they supply Android components like
          // ContentProviders)
          && component.hasNoManifestMatches()
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
    val withTransitives = LinkedHashMap<Dependency, MutableSet<Dependency>>().apply {
      // Seed with unused dependencies because final collection is expected to contain one entry per
      // unused dep.
      putAll(unusedDeps.map { it to mutableSetOf() })
    }
    val visited = mutableSetOf<String>()

    fun walk(root: ResolvedComponentResult) {
      val rootId = root.id.asString()
      // we map our current `root` to a known declared dependency (may be null if the root is not a
      // declared dependency).
      val rootComponent = declaredComponents.find { it.dependency.identifier == rootId }

      root.dependencies
        .filterIsInstance<ResolvedDependencyResult>()
        // AGP adds all runtime dependencies as constraints to the compile classpath, and these show
        // up in the resolution result. Filter them out.
        .filterNot { it.isConstraint }
        // For similar reasons as above
        .filterNot { it.isJavaPlatform() }
        .forEach { dependencyResult ->
          val depId = dependencyResult.selected.id.asString()
          if (!visited.contains(depId)) {
            visited.add(depId)
            // recursively walk the graph in a depth-first pattern
            walk(dependencyResult.selected)
          }

          if (rootComponent != null && usedTransitiveComponents.contains(depId)) {
            val dep = Dependency(
              identifier = depId,
              resolvedVersion = dependencyResult.selected.id.resolvedVersion()
            )
            withTransitives.merge(rootComponent.dependency, mutableSetOf(dep)) { acc, inc ->
              acc.apply { addAll(inc) }
            }
          }
        }
    }
    walk(root)
    val declaredComponentsWithTransitives = withTransitives.map { (key, value) ->
      val trans = if (value.isNotEmpty()) value else null
      ComponentWithTransitives(key, trans)
    }.toSet()

    // Filter above to only get those that are unused
    val unusedDepsWithTransitives: Set<ComponentWithTransitives> =
      declaredComponentsWithTransitives.filterToSet { comp ->
        unusedDeps.any { it == comp.dependency }
      }

    return DependencyReport(
      allComponentsWithTransitives = declaredComponentsWithTransitives,
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

  private fun Component.hasNoNativeLibUsages(): Boolean {
    return nativeLibDependencies?.none { it.dependency == dependency } ?: true
  }

  private fun Component.hasNoConstantUsages(): Boolean {
    return usedConstantDependencies.none { it == dependency }
  }

  private fun Component.hasNoGeneralUsages(): Boolean {
    return usedGenerally.none { it == dependency }
  }

  /**
   * If the component's dependency matches any of our manifest dependencies, and that manifest provides an Android
   * component, then it is used.
   */
  private fun Component.hasNoManifestMatches(): Boolean {
    val manifest = manifests?.find { it.dependency == dependency } ?: return true
    return manifest.componentMap.isEmpty()
  }

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

/**
 * Returns true if any of the variants are a kind of platform.
 */
private fun ResolvedDependencyResult.isJavaPlatform(): Boolean = selected.variants.any { variant ->
  val category = variant.attributes.getAttribute(CATEGORY)
  category == Category.REGULAR_PLATFORM || category == Category.ENFORCED_PLATFORM
}

/**
 * This is different than [org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE], which has type
 * `Category` (cf `String`).
 */
private val CATEGORY = Attribute.of("org.gradle.category", String::class.java)
