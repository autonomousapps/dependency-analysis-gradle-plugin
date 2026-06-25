// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.utils.*
import com.autonomousapps.internal.utils.strings.ensureSuffix
import com.autonomousapps.model.*
import com.autonomousapps.model.internal.BinaryClassCapability
import com.autonomousapps.model.internal.Dependency
import com.autonomousapps.model.internal.InlineMemberCapability
import com.autonomousapps.model.internal.ProjectVariant
import com.autonomousapps.model.internal.intermediates.producer.ExplodedJar
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.util.*
import javax.inject.Inject

@CacheableTask
public abstract class ComputeTypeUsageTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    description = "Computes type-level dependency usage"
  }

  @get:Input
  public abstract val buildPath: Property<String>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val syntheticProject: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val explodedJars: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputDirectory
  public abstract val dependencies: DirectoryProperty

  @get:Input
  public abstract val excludedPackages: SetProperty<String>

  @get:Input
  public abstract val excludedTypes: SetProperty<String>

  @get:Input
  public abstract val excludedRegexPatterns: ListProperty<String>

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction
  public fun action() {
    workerExecutor.noIsolation().submit(ComputeTypeUsageAction::class.java) {
      it.buildPath.set(buildPath)
      it.syntheticProject.set(syntheticProject)
      it.explodedJars.set(explodedJars)
      it.dependencies.set(dependencies)
      it.excludedPackages.set(excludedPackages)
      it.excludedTypes.set(excludedTypes)
      it.excludedRegexPatterns.set(excludedRegexPatterns)
      it.output.set(output)
    }
  }

  public interface ComputeTypeUsageParameters : WorkParameters {
    public val buildPath: Property<String>
    public val syntheticProject: RegularFileProperty
    public val explodedJars: RegularFileProperty
    public val dependencies: DirectoryProperty
    public val excludedPackages: SetProperty<String>
    public val excludedTypes: SetProperty<String>
    public val excludedRegexPatterns: ListProperty<String>
    public val output: RegularFileProperty
  }

  public abstract class ComputeTypeUsageAction : WorkAction<ComputeTypeUsageParameters> {

    private val buildPath = parameters.buildPath.get()

    override fun execute() {
      val output = parameters.output.getAndDelete()

      // 1. Load data
      val project = parameters.syntheticProject.fromJson<ProjectVariant>(compressed = true)
      val dependencies = project.dependencies(parameters.dependencies.get())
      val classToCoords = buildClassIndex()

      // 2. Build filter
      val filter = TypeFilter(
        excludedPackages = parameters.excludedPackages.get(),
        excludedTypes = parameters.excludedTypes.get(),
        excludedRegexPatterns = parameters.excludedRegexPatterns.get(),
      )

      // 3. Analyze usage
      val analyzer = TypeUsageAnalyzer(project, classToCoords, filter, dependencies)
      val typeUsage = analyzer.analyze()

      // 4. Write output
      output.bufferWriteJson(typeUsage)
    }

    private fun buildClassIndex(): Map<String, Coordinates> {
      val explodedJars = parameters.explodedJars.fromJsonSet<ExplodedJar>(compressed = true)

      val map = mutableMapOf<String, Coordinates>()
      explodedJars.forEach { jar ->
        val coordinates = jar.coordinates.normalized(buildPath)
        jar.binaryClasses.forEach { binaryClass ->
          map[binaryClass.className] = coordinates
        }
      }

      return map
    }
  }
}

private class TypeFilter(
  excludedPackages: Set<String>,
  val excludedTypes: Set<String>,
  excludedRegexPatterns: List<String>,
) {
  private val normalizedPackages = excludedPackages.mapToSet { it.ensureSuffix(".") }
  private val compiledPatterns = excludedRegexPatterns.map { it.toRegex() }

  fun shouldExclude(className: String): Boolean {
    if (excludedTypes.contains(className)) return true
    if (normalizedPackages.any { className.startsWith(it) }) return true
    if (compiledPatterns.any { it.matches(className) }) return true
    return false
  }
}

private class TypeUsageAnalyzer(
  private val project: ProjectVariant,
  private val classToCoords: Map<String, Coordinates>,
  private val filter: TypeFilter,
  private val dependencies: Set<Dependency>,
) {

  fun analyze(): ProjectTypeUsage {
    val usageMap = mutableMapOf<Coordinates, MutableMap<String, Int>>()
    val unknown = mutableMapOf<String, Int>()

    // Get project's own class names for internal type detection
    val projectClasses = project.classNames

    // Count usage per class per dependency (both non-annotation and annotation classes)
    project.codeSource.forEach { source ->
      val allUsedClasses = source.usedNonAnnotationClasses + source.usedAnnotationClasses

      allUsedClasses
        // classes referenced via inline functions, etc.
        .plus(findUsedImports(source.imports))
        .filterNot(filter::shouldExclude)
        // Probably from the imports
        .filterNot { typeName -> typeName == "null" }
        .forEach { typeName ->
          // Determine coordinates: check project classes first, then external
          val coords = classToCoords[typeName]

          if (coords != null || projectClasses.contains(typeName)) {
            usageMap
              .getOrPut(coords ?: project.coordinates) { mutableMapOf() }
              .merge(typeName, 1, Int::plus)
          } else {
            unknown.merge(typeName, 1, Int::plus)
          }
        }
    }

    // Categorize dependencies
    val internal = sortedMapOf<String, Int>()
    val projectDeps = sortedMapOf<String, Map<String, Int>>()
    val libraryDeps = sortedMapOf<String, Map<String, Int>>()

    usageMap.forEach { (coords, typeUsages) ->
      when (coords) {
        project.coordinates -> internal.putAll(typeUsages)
        is ProjectCoordinates -> projectDeps[coords.identifier] = typeUsages.toSortedMap()
        is ModuleCoordinates -> libraryDeps[coords.identifier] = typeUsages.toSortedMap()
        else -> libraryDeps[coords.identifier] = typeUsages
      }
    }

    return ProjectTypeUsage(
      projectPath = project.coordinates.identifier,
      summary = TypeUsageSummary(
        totalTypes = usageMap.values.sumOf { it.size },
        totalFiles = project.codeSource.size,
        internalTypes = internal.size,
        projectDependencies = projectDeps.size,
        libraryDependencies = libraryDeps.size,
      ),
      internal = internal.toSortedMap(),
      projectDependencies = projectDeps.toSortedMap(),
      libraryDependencies = libraryDeps.toSortedMap(),
      unknownDependencies = unknown.toSortedMap(),
    )
  }

  // dependency -> imports
  private val importsCache = mutableMapOf<String, Set<Imports>>()

  // mapping imports to their "Kt" class names
  private fun findUsedImports(imports: Set<String>): Set<String> {
    return dependencies
      .flatMapToOrderedSet { dependency ->
        getCacheEntry(dependency)
          .asSequence()
          .filter { entry -> entry.candidateImports.any { it in imports } }
          .mapTo(TreeSet()) { entry -> entry.className }
      }
  }

  private fun getCacheEntry(dependency: Dependency): Set<Imports> {
    // We use the String coordinates as the map key because `dependency.hashCode()` is EXTREMELY expensive.
    return importsCache.getOrPut(dependency.coordinates.gav()) {
      val binaryEntries = dependency.findCapability<BinaryClassCapability>()?.let(Imports::of).orEmpty()
      val inlineEntries = dependency.findCapability<InlineMemberCapability>()?.let(Imports::of).orEmpty()

      binaryEntries + inlineEntries
    }
  }

  /**
   * Maps class names to import statements. Currently used for inline functions and source-retained annotations.
   *
   * For inline functions, given a source file `com/foo/Bar.kt`, which produces a class file `com/foo/Bar.class`:
   * ```
   * inline fun inlineFun1() { ... }
   *
   * inline fun inlineFun2() { ... }
   * ```
   *
   * Imports in other sources files will look like:
   * ```
   * import com.foo.inlineFun1
   * import com.foo.*
   * ```
   */
  private data class Imports(
    /** com.foo.BarKt */
    val className: String,
    /** list: `com.foo.inlineFun1`, `com.foo.inlineFun1`, `com.foo.SourceRetainedAnnotation`. */
    val candidateImports: Set<String>,
  ) : Comparable<Imports> {
    override fun compareTo(other: Imports): Int {
      return compareBy(Imports::className)
        .thenBy(LexicographicIterableComparator()) { it.candidateImports }
        .compare(this, other)
    }

    companion object {
      fun of(inlineMember: InlineMemberCapability): Set<Imports> {
        return inlineMember.inlineMembers.mapToOrderedSet(Imports::of)
      }

      fun of(inlineMember: InlineMemberCapability.InlineMember): Imports {
        return Imports(
          className = inlineMember.className,
          candidateImports = inlineMember.candidateImports(),
        )
      }

      fun of(binary: BinaryClassCapability): Set<Imports> {
        return binary.classes.mapToOrderedSet { className ->
          val candidateImports = if (className.contains('.')) {
            sortedSetOf(className, className.substringBeforeLast('.') + ".*")
          } else {
            sortedSetOf(className)
          }

          Imports(
            className = className,
            candidateImports = candidateImports,
          )
        }
      }
    }
  }
}
