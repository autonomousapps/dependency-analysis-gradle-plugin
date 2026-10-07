// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.advice.TransitiveExposureIndex
import com.autonomousapps.internal.utils.bufferWriteJson
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.IncludedBuildCoordinates
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.model.internal.AggregateTypeUsageReport
import com.autonomousapps.model.internal.ProjectMetadata
import com.autonomousapps.model.internal.PublicTypes
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * Suppresses "remove" advice for project dependencies that downstream consumers reach transitively.
 *
 * A dependency `D` declared by project `A` is _transitively exposed_ when some project `B` depends on `A` and uses
 * types that `D` provides. Removing `D` from `A` would then break `B`, even though `A` itself never references `D`.
 *
 * Runs at the root, after per-project analysis. Opt-in: see `dependency.analysis.transitive.exposure`.
 */
@CacheableTask
public abstract class FilterTransitiveExposureTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    description = "Suppresses removal advice for dependencies that downstream consumers use transitively"
  }

  /** Per-project [ProjectAdvice] reports. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val projectHealthReports: ConfigurableFileCollection

  /** Per-project [AggregateTypeUsageReport]s: which types each project uses, and from which dependency. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val typeUsageReports: ConfigurableFileCollection

  /** Per-project [PublicTypes] reports: which types each project exposes to its consumers. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val publicClassesReports: ConfigurableFileCollection

  /** Per-project [ProjectMetadata], which marks shadow/fat-jar modules. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val projectMetadataReports: ConfigurableFileCollection

  /**
   * How many hops down the consumer graph to search. 1 checks direct consumers only, which is the common case; higher
   * values catch chains such as `A -> B -> C` where only `C` uses the exposed types.
   */
  @get:Input
  public abstract val transitiveDepth: Property<Int>

  @get:OutputDirectory
  public abstract val outputDir: DirectoryProperty

  @TaskAction
  public fun action() {
    workerExecutor.noIsolation().submit(Action::class.java) {
      it.projectHealthReports.setFrom(projectHealthReports)
      it.typeUsageReports.setFrom(typeUsageReports)
      it.publicClassesReports.setFrom(publicClassesReports)
      it.projectMetadataReports.setFrom(projectMetadataReports)
      it.transitiveDepth.set(transitiveDepth)
      it.outputDir.set(outputDir)
    }
  }

  public interface Parameters : WorkParameters {
    public val projectHealthReports: ConfigurableFileCollection
    public val typeUsageReports: ConfigurableFileCollection
    public val publicClassesReports: ConfigurableFileCollection
    public val projectMetadataReports: ConfigurableFileCollection
    public val transitiveDepth: Property<Int>
    public val outputDir: DirectoryProperty
  }

  public abstract class Action : WorkAction<Parameters> {

    private val logger = Logging.getLogger(FilterTransitiveExposureTask::class.java)

    override fun execute() {
      val outDir = parameters.outputDir.get().asFile

      val index = TransitiveExposureIndex.of(
        typeUsages = parameters.typeUsageReports.files
          .filter { it.isFile && it.length() > 0 }
          .map { it.fromJson<AggregateTypeUsageReport>() },
        publicTypes = parameters.publicClassesReports.files
          .filter { it.isFile && it.length() > 0 }
          .map { it.fromJson<PublicTypes>() },
        maxDepth = parameters.transitiveDepth.get(),
      )

      // Shadow/fat-jar modules bundle their dependencies into the artifact, so they legitimately declare
      // dependencies they never reference. Every removal suggestion for one is a false positive.
      val assemblyProjects = parameters.projectMetadataReports.files
        .filter { it.isFile && it.length() > 0 }
        .map { it.fromJson<ProjectMetadata>() }
        .filter { it.isAssembly }
        .mapTo(mutableSetOf()) { it.projectPath }

      var suppressed = 0
      var assemblySuppressed = 0

      parameters.projectHealthReports.files
        .filter { it.isFile && it.length() > 0 }
        .map { it.fromJson<ProjectAdvice>() }
        .forEach { projectAdvice ->
          val isAssembly = projectAdvice.projectPath in assemblyProjects

          val kept = projectAdvice.dependencyAdvice.filterTo(mutableSetOf()) { advice ->
            if (isAssembly && advice.isAnyRemove()) {
              assemblySuppressed++
              return@filterTo false
            }

            val dependencyProject = advice.coordinates.projectPathOrNull()
            val keep = !advice.isAnyRemove() ||
              dependencyProject == null ||
              !(index.isTransitivelyExposed(projectAdvice.projectPath, dependencyProject) ||
                index.isExposedThroughDependencyAbi(projectAdvice.projectPath, dependencyProject))

            if (!keep) suppressed++
            keep
          }

          // Name outputs by project path, not by iteration index: `projectHealthReports.files` is an
          // unordered Set, and this is a @CacheableTask.
          outDir.resolve(projectAdvice.projectPath.toFileName())
            .bufferWriteJson(projectAdvice.copy(dependencyAdvice = kept))
        }

      if (suppressed > 0) {
        logger.lifecycle("Transitive exposure filter: suppressed $suppressed removal suggestion(s).")
      }
      if (assemblySuppressed > 0) {
        logger.lifecycle("Assembly modules: suppressed $assemblySuppressed removal suggestion(s).")
      }
    }

    /**
     * The project path these coordinates point at, or null if they name an external library. Only project
     * dependencies are considered: an external library is not exposed to downstream consumers the same way.
     */
    private fun Coordinates.projectPathOrNull(): String? = when (this) {
      is ProjectCoordinates -> identifier
      is IncludedBuildCoordinates -> resolvedProject.identifier
      else -> null
    }

    /** `":foo:bar"` -> `"foo-bar.json"`; the root project -> `"root.json"`. */
    private fun String.toFileName(): String {
      val name = trim(':').replace(':', '-').ifEmpty { "root" }
      return "$name.json"
    }
  }
}
