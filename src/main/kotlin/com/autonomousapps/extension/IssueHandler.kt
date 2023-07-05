@file:Suppress("UnstableApiUsage", "unused")

package com.autonomousapps.extension

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import javax.inject.Inject

/**
 * Defers to `all` and `projects` for customization behavior.
 *
 * ```
 * dependencyAnalysis {
 *   issues {
 *     all { ... }
 *     project(':lib') { ... }
 *   }
 * }
 * ```
 */
open class IssueHandler @Inject constructor(objects: ObjectFactory) {

  private val all = objects.newInstance(ProjectIssueHandler::class.java, "__all")
  private val projects = objects.domainObjectContainer(ProjectIssueHandler::class.java)

  fun all(action: Action<ProjectIssueHandler>) {
    action.execute(all)
  }

  fun project(path: String, action: Action<ProjectIssueHandler>) {
    projects.maybeCreate(path).apply {
      action.execute(this)
    }
  }

  private fun wrapException(e: GradleException) = if (e is InvalidUserDataException)
    GradleException("You must configure this project either at the root or the project level, not both", e)
  else e

  internal fun ignoreKtxFor(path: String): Provider<Boolean> {
    val global = all.ignoreKtx
    val proj = projects.findByName(path)?.ignoreKtx

    // If there's no project-specific handler, just return the global handler
    return if (proj == null) {
      global
    } else {
      // If there is a project-specific handler, union it with the global handler, returning true if
      // either is true.
      global.flatMap { g ->
        proj.map { p ->
          g || p
        }
      }
    }
  }

  internal fun shouldAnalyzeSourceSet(sourceSetName: String, projectPath: String): Boolean {
    val a = sourceSetName !in all.ignoreSourceSets.get()
    val b = sourceSetName !in projects.findByName(projectPath)?.ignoreSourceSets?.get().orEmpty()

    return a && b
  }

  internal fun anyIssueFor(path: String): Provider<Behavior> {
    val global = all.anyIssue
    val proj = projects.findByName(path)?.anyIssue
    return overlay(global, proj)
  }

  internal fun unusedDependenciesIssueFor(path: String): Provider<Behavior> {
    val global = all.unusedDependenciesIssue
    val proj = projects.findByName(path)?.unusedDependenciesIssue
    return overlay(global, proj)
  }

  internal fun usedTransitiveDependenciesIssueFor(path: String): Provider<Behavior> {
    val global = all.usedTransitiveDependenciesIssue
    val proj = projects.findByName(path)?.usedTransitiveDependenciesIssue
    return overlay(global, proj)
  }

  internal fun incorrectConfigurationIssueFor(path: String): Provider<Behavior> {
    val global = all.incorrectConfigurationIssue
    val proj = projects.findByName(path)?.incorrectConfigurationIssue
    return overlay(global, proj)
  }

  internal fun compileOnlyIssueFor(path: String): Provider<Behavior> {
    val global = all.compileOnlyIssue
    val proj = projects.findByName(path)?.compileOnlyIssue
    return overlay(global, proj)
  }

  internal fun runtimeOnlyIssueFor(path: String): Provider<Behavior> {
    val global = all.runtimeOnlyIssue
    val proj = projects.findByName(path)?.runtimeOnlyIssue
    return overlay(global, proj)
  }

  internal fun unusedAnnotationProcessorsIssueFor(path: String): Provider<Behavior> {
    val global = all.unusedAnnotationProcessorsIssue
    val proj = projects.findByName(path)?.unusedAnnotationProcessorsIssue
    return overlay(global, proj)
  }

  internal fun redundantPluginsIssueFor(path: String): Provider<Behavior> {
    val global = all.redundantPluginsIssue
    val proj = projects.findByName(path)?.redundantPluginsIssue
    return overlay(global, proj)
  }

  internal fun moduleStructureIssueFor(path: String): Provider<Behavior> {
    val global = all.moduleStructureIssue
    val proj = projects.findByName(path)?.moduleStructureIssue
    return overlay(global, proj)
  }

  /** Project severity wins over global severity. Excludes are unioned. */
  private fun overlay(global: Issue, project: Issue?): Provider<Behavior> {
    // If there's no project-specific handler, just return the global handler
    return if (project == null) {
      global.behavior().map { g ->
        if (g is Undefined) Warn(g.filter) else g
      }
    } else {
      global.behavior().flatMap { g ->
        val allFilter = g.filter
        project.behavior().map { p ->
          val projFilter = p.filter
          val union = allFilter + projFilter

          when (p) {
            is Fail -> Fail(union)
            is Warn -> Warn(union)
            is Ignore -> Ignore
            is Undefined -> {
              when (g) {
                is Fail -> Fail(union)
                is Warn -> Warn(union)
                is Undefined -> Warn(union)
                is Ignore -> Ignore
              }
            }
          }
        }
      }
    }
  }
}
