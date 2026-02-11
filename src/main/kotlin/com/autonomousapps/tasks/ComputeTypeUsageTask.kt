// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.utils.bufferWriteJson
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.getJsonListAdapter
import com.autonomousapps.model.ProjectTypeUsage
import com.autonomousapps.model.TypeUsageSummary
import com.autonomousapps.model.internal.intermediates.producer.ExplodedJar
import com.autonomousapps.model.internal.ProjectVariant
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import okio.buffer
import okio.source
import java.io.File
import java.util.zip.GZIPInputStream
import javax.inject.Inject

@CacheableTask
public abstract class ComputeTypeUsageTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    description = "Computes type-level dependency usage"
  }

  @get:Input
  public abstract val projectPath: Property<String>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val syntheticProject: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val explodedJars: RegularFileProperty

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
      it.projectPath.set(projectPath)
      it.syntheticProject.set(syntheticProject)
      it.explodedJars.set(explodedJars)
      it.excludedPackages.set(excludedPackages)
      it.excludedTypes.set(excludedTypes)
      it.excludedRegexPatterns.set(excludedRegexPatterns)
      it.output.set(output)
    }
  }

  public interface ComputeTypeUsageParameters : WorkParameters {
    public val projectPath: Property<String>
    public val syntheticProject: RegularFileProperty
    public val explodedJars: RegularFileProperty
    public val excludedPackages: SetProperty<String>
    public val excludedTypes: SetProperty<String>
    public val excludedRegexPatterns: ListProperty<String>
    public val output: RegularFileProperty
  }

  public abstract class ComputeTypeUsageAction : WorkAction<ComputeTypeUsageParameters> {

    override fun execute() {
      val output = parameters.output.getAndDelete()

      // 1. Load data
      val project = parameters.syntheticProject.fromJson<ProjectVariant>()
      val classToCoords = buildClassIndex(parameters.explodedJars.get().asFile)

      // 2. Build filter
      val filter = TypeFilter(
        excludedPackages = parameters.excludedPackages.get(),
        excludedTypes = parameters.excludedTypes.get(),
        excludedRegexPatterns = parameters.excludedRegexPatterns.get()
      )

      // 3. Analyze usage
      val analyzer = TypeUsageAnalyzer(project, classToCoords, filter)
      val typeUsage = analyzer.analyze()

      // 4. Write output
      output.bufferWriteJson(typeUsage)
    }

    private fun buildClassIndex(explodedJarsFile: File): Map<String, String> {
      if (!explodedJarsFile.exists()) return emptyMap()

      val map = mutableMapOf<String, String>()

      GZIPInputStream(explodedJarsFile.inputStream()).use { gzipStream ->
        val explodedJars = gzipStream.source().buffer().use { bufferedSource ->
          getJsonListAdapter<ExplodedJar>().fromJson(bufferedSource)!!
        }

        explodedJars.forEach { jar ->
          // Normalize identifier to handle included builds (convert "build:project" to ":project")
          val identifier = jar.coordinates.normalizedIdentifier(":")
          jar.binaryClasses.forEach { binaryClass ->
            map[binaryClass.className] = identifier
          }
        }
      }

      return map
    }
  }
}

private class TypeFilter(
  val excludedPackages: Set<String>,
  val excludedTypes: Set<String>,
  excludedRegexPatterns: List<String>
) {
  private val compiledPatterns = excludedRegexPatterns.map { it.toRegex() }

  fun shouldExclude(className: String): Boolean {
    if (excludedTypes.contains(className)) return true
    if (excludedPackages.any { className.startsWith("$it.") }) return true
    if (compiledPatterns.any { it.matches(className) }) return true
    return false
  }
}

private class TypeUsageAnalyzer(
  private val project: ProjectVariant,
  private val classToCoords: Map<String, String>,
  private val filter: TypeFilter
) {
  fun analyze(): ProjectTypeUsage {
    val usageMap = mutableMapOf<String, MutableMap<String, Int>>()

    // Get project's own class names for internal type detection
    val projectClasses = project.classNames

    // Count usage per class per dependency (both non-annotation and annotation classes)
    project.codeSource.forEach { source ->
      val allUsedClasses = source.usedNonAnnotationClasses + source.usedAnnotationClasses

      allUsedClasses.forEach { className ->
        if (filter.shouldExclude(className)) return@forEach

        // Determine coordinates: check project classes first, then external
        val coords = when {
          projectClasses.contains(className) -> project.coordinates.identifier
          else -> classToCoords[className] ?: "UNKNOWN"
        }

        usageMap
          .getOrPut(coords) { mutableMapOf() }
          .merge(className, 1, Int::plus)
      }
    }

    // Categorize dependencies
    val internal = mutableMapOf<String, Int>()
    val projectDeps = mutableMapOf<String, Map<String, Int>>()
    val libraryDeps = mutableMapOf<String, Map<String, Int>>()

    usageMap.forEach { (coords, typeUsages) ->
      when {
        coords == project.coordinates.identifier -> internal.putAll(typeUsages)
        coords.startsWith(":") -> projectDeps[coords] = typeUsages
        coords != "UNKNOWN" -> libraryDeps[coords] = typeUsages
      }
    }

    return ProjectTypeUsage(
      projectPath = project.coordinates.identifier,
      summary = TypeUsageSummary(
        totalTypes = usageMap.values.sumOf { it.size },
        totalFiles = project.codeSource.size,
        internalTypes = internal.size,
        projectDependencies = projectDeps.size,
        libraryDependencies = libraryDeps.size
      ),
      internal = internal,
      projectDependencies = projectDeps,
      libraryDependencies = libraryDeps
    )
  }
}
