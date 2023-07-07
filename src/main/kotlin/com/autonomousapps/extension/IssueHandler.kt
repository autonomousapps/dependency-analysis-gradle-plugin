@file:Suppress("UnstableApiUsage", "unused")

package com.autonomousapps.extension

import com.autonomousapps.internal.utils.mapToMutableList
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
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

  private val undefined = objects.newInstance<Issue>()
  private val defaultBehavior = objects.property<Behavior>().convention(Warn())

  private val all = objects.newInstance(ProjectIssueHandler::class.java, "__all")
  private val projects = objects.domainObjectContainer(ProjectIssueHandler::class.java)

  fun all(action: Action<ProjectIssueHandler>) {
    action.execute(all)
  }

  fun project(projectPath: String, action: Action<ProjectIssueHandler>) {
    projects.maybeCreate(projectPath).apply {
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

  internal fun anyIssueFor(projectPath: String): List<Provider<Behavior>> {
    return issuesFor(projectPath) { it.anyIssue }
  }

  internal fun unusedDependenciesIssueFor(projectPath: String): List<Provider<Behavior>> {
    return issuesFor(projectPath) { it.unusedDependenciesIssue }
  }

  internal fun usedTransitiveDependenciesIssueFor(projectPath: String): List<Provider<Behavior>> {
    return issuesFor(projectPath) { it.usedTransitiveDependenciesIssue }
  }

  internal fun incorrectConfigurationIssueFor(projectPath: String): List<Provider<Behavior>> {
    return issuesFor(projectPath) { it.incorrectConfigurationIssue }
  }

  internal fun compileOnlyIssueFor(projectPath: String): List<Provider<Behavior>> {
    return issuesFor(projectPath) { it.compileOnlyIssue }
  }

  internal fun runtimeOnlyIssueFor(projectPath: String): List<Provider<Behavior>> {
    return issuesFor(projectPath) { it.runtimeOnlyIssue }
  }

  internal fun unusedAnnotationProcessorsIssueFor(projectPath: String): List<Provider<Behavior>> {
    return issuesFor(projectPath) { it.unusedAnnotationProcessorsIssue }
  }

  internal fun redundantPluginsIssueFor(projectPath: String): Provider<Behavior> {
    return overlay(all.redundantPluginsIssue, projects.findByName(projectPath)?.redundantPluginsIssue)
  }

  internal fun moduleStructureIssueFor(projectPath: String): Provider<Behavior> {
    return overlay(all.moduleStructureIssue, projects.findByName(projectPath)?.moduleStructureIssue)
  }

  private fun issuesFor(projectPath: String, mapper: (ProjectIssueHandler) -> Issue): List<Provider<Behavior>> {
    val projectHandler = projects.findByName(projectPath)

    val allIssuesBySourceSet = all.issuesBySourceSet(mapper)
    val projectIssuesBySourceSet = projectHandler.issuesBySourceSet(mapper)
    val globalProjectMatches = mutableListOf<Pair<Issue, Issue?>>()

    // Iterate through the global/all list first
    val iter = allIssuesBySourceSet.iterator()
    while (iter.hasNext()) {
      // Find matching (by sourceSetName) elements in both lists
      val a = iter.next()
      val b = projectIssuesBySourceSet.find { p -> p.sourceSet.get() == a.sourceSet.get() }

      // Drain both lists
      iter.remove()
      if (b != null) projectIssuesBySourceSet.remove(b)

      // Add to result list (it's ok if `b` is null)
      globalProjectMatches.add(a to b)
    }

    // Now iterate through the remaining elements of the proj list
    projectIssuesBySourceSet.forEach { b ->
      val a = allIssuesBySourceSet.find { a -> a.sourceSet.get() == b.sourceSet.get() }

      // In contrast to the above, it is NOT ok if `a` is null, so we use `undefined` instead.
      if (a != null) {
        globalProjectMatches.add(a to b)
      } else {
        globalProjectMatches.add(undefined to b)
      }
    }

    val primaryBehavior = overlay(mapper(all), projectHandler?.let(mapper))
    val result = mutableListOf(primaryBehavior)
    globalProjectMatches.mapTo(result) { (global, project) ->
      overlay(global, project, primaryBehavior)
    }

    return result
  }

  private fun ProjectIssueHandler?.issuesBySourceSet(mapper: (ProjectIssueHandler) -> Issue): MutableList<Issue> {
    return this?.sourceSets.mapToMutableList { s ->
      s.issueOf(mapper)
    }
  }

  private fun issuesBySourceSetFor(project: ProjectIssueHandler?, mapper: (ProjectIssueHandler) -> Issue): List<Issue> {
    return all.issuesBySourceSet(mapper) + project.issuesBySourceSet(mapper)
  }

  /** Project severity wins over global severity. Excludes are unioned. */
  private fun overlay(global: Issue, project: Issue?, coerceTo: Provider<Behavior>? = null): Provider<Behavior> {
    val c = coerceTo ?: defaultBehavior

    // If there's no project-specific handler, just return the global handler
    return if (project == null) {
      c.flatMap { coerce ->
        global.behavior().map { g ->
          if (g is Undefined) {
            when (coerce) {
              is Fail -> Fail(filter = g.filter, sourceSetName = g.sourceSetName)
              is Warn, is Undefined -> Warn(filter = g.filter, sourceSetName = g.sourceSetName)
              is Ignore -> Ignore(sourceSetName = g.sourceSetName)
            }
          } else {
            g
          }
        }
      }
    } else {
      // TODO coerce?

      global.behavior().flatMap { g ->
        val allFilter = g.filter
        project.behavior().map { p ->
          val projFilter = p.filter
          val union = allFilter + projFilter

          when (p) {
            is Fail -> Fail(filter = union, sourceSetName = p.sourceSetName)
            is Warn -> Warn(filter = union, sourceSetName = p.sourceSetName)
            is Ignore -> Ignore(sourceSetName = p.sourceSetName)
            is Undefined -> {
              when (g) {
                is Fail -> Fail(filter = union, sourceSetName = p.sourceSetName)
                is Warn -> Warn(filter = union, sourceSetName = p.sourceSetName)
                is Undefined -> Warn(filter = union, sourceSetName = p.sourceSetName)
                is Ignore -> Ignore(sourceSetName = p.sourceSetName)
              }
            }
          }
        }
      }
    }
  }
}
