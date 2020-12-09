@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.*
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class AdviceAggregateReportTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Aggregates advice reports across all subprojects"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  lateinit var adviceAllReports: Configuration

  @get:OutputFile
  abstract val projectReport: RegularFileProperty

  @get:OutputFile
  abstract val projectReportPretty: RegularFileProperty

  @get:OutputFile
  abstract val rippleReport: RegularFileProperty

  @TaskAction
  fun action() {
    val projectReportFile = projectReport.getAndDelete()
    val projectReportPrettyFile = projectReportPretty.getAndDelete()
    val rippleFile = rippleReport.getAndDelete()

    val comprehensiveAdvice = adviceAllReports.dependencies
      // They should all be project dependencies, but
      // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/295
      .filterIsInstance<ProjectDependency>()
      .map { dependency ->
        val path = dependency.dependencyProject.path

        val compAdvice: Set<ComprehensiveAdvice> = adviceAllReports.fileCollection(dependency)
          .filter { it.exists() }
          .mapToSet { it.readText().fromJson() }

        path to compAdvice.toMutableSet()
      }.mergedMap()

    // TODO the below could all go in a WorkAction
    val buildHealth = comprehensiveAdvice.map { (path, advice) ->
      ComprehensiveAdvice(
        projectPath = path,
        dependencyAdvice = advice.flatMapToSet { it.dependencyAdvice },
        pluginAdvice = advice.flatMapToSet { it.pluginAdvice },
        shouldFail = advice.any { it.shouldFail }
      )
    }

    val ripples = computeRipples(buildHealth)

    projectReportFile.writeText(buildHealth.toJson())
    projectReportPrettyFile.writeText(buildHealth.toPrettyString())
    rippleFile.writeText(ripples.toJson())

    if (buildHealth.any { it.isNotEmpty() }) {
      logger.debug("Build health report (aggregated) : ${projectReportFile.path}")
      logger.debug("(pretty-printed)                 : ${projectReportPrettyFile.path}")
    }
  }

  private fun computeRipples(buildHealth: List<ComprehensiveAdvice>): List<Ripple> {
    val upgrades = mutableListOf<Pair<String, DownstreamImpact>>()
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
              upgrades.add(compAdvice.projectPath to DownstreamImpact(
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
    downgrades.forEach { rippleCandidate ->
      upgrades.filter { (_, impact) ->
        impact.sourceProjectPath == rippleCandidate.projectPath
          && impact.providedDependency == rippleCandidate.providedDependency
      }.forEach { (_, impact) ->
        ripples.add(Ripple(
          upstreamSource = rippleCandidate,
          downstreamImpact = impact
        ))
      }
    }
    return ripples
  }

  /**
   * If this is advice to remove or downgrade an api-like dependency.
   */
  private fun Advice.isDowngrade(): Boolean {
    return (isRemove() || isChange() || isCompileOnly())
      && dependency.configurationName?.endsWith("api", ignoreCase = true) == true
  }
}
