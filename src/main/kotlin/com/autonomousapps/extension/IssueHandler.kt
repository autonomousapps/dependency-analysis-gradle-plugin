@file:Suppress("UnstableApiUsage", "unused")

package com.autonomousapps.extension

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import java.io.Serializable
import javax.inject.Inject

/**
 * ```
 * dependencyAnalysis {
 *   issues {
 *     all {
 *       onAny {
 *         severity(<'fail'|'warn'|'ignore'>)
 *         exclude('an:external-dep', 'another:external-dep', ':a:project-dep')
 *       }
 *       onUnusedDependencies { ... }
 *       onUsedTransitiveDependencies { ... }
 *       onIncorrectConfiguration { ... }
 *       onRedundantPlugins { ... }
 *
 *       ignoreKtx(<true|false>) // default is false
 *     }
 *     project(':lib') {
 *       ...
 *     }
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

  internal fun anyIssue(): Provider<Behavior> = all.anyIssue.behavior()
  internal fun unusedDependenciesIssue(): Provider<Behavior> = all.unusedDependenciesIssue.behavior()
  internal fun usedTransitiveDependenciesIssue(): Provider<Behavior> = all.usedTransitiveDependenciesIssue.behavior()
  internal fun incorrectConfigurationIssue(): Provider<Behavior> = all.incorrectConfigurationIssue.behavior()
  internal fun compileOnlyIssue(): Provider<Behavior> = all.compileOnlyIssue.behavior()
  internal fun runtimeOnlyIssue(): Provider<Behavior> = all.runtimeOnlyIssue.behavior()
  internal fun unusedAnnotationProcessorsIssue(): Provider<Behavior> = all.unusedAnnotationProcessorsIssue.behavior()
  internal fun redundantPluginsIssue(): Provider<Behavior> = all.redundantPluginsIssue.behavior()

  internal fun anyIssueFor(path: String): Provider<Behavior> {
    val global = all.anyIssue
    val proj = projects.findByName(path)?.anyIssue
    return union(global, proj)
  }

  internal fun unusedDependenciesIssueFor(path: String): Provider<Behavior> {
    val global = all.unusedDependenciesIssue
    val proj = projects.findByName(path)?.unusedDependenciesIssue
    return union(global, proj)
  }

  internal fun usedTransitiveDependenciesIssueFor(path: String): Provider<Behavior> {
    val global = all.usedTransitiveDependenciesIssue
    val proj = projects.findByName(path)?.usedTransitiveDependenciesIssue
    return union(global, proj)
  }

  internal fun incorrectConfigurationIssueFor(path: String): Provider<Behavior> {
    val global = all.incorrectConfigurationIssue
    val proj = projects.findByName(path)?.incorrectConfigurationIssue
    return union(global, proj)
  }

  internal fun compileOnlyIssueFor(path: String): Provider<Behavior> {
    val global = all.compileOnlyIssue
    val proj = projects.findByName(path)?.compileOnlyIssue
    return union(global, proj)
  }

  internal fun runtimeOnlyIssueFor(path: String): Provider<Behavior> {
    val global = all.runtimeOnlyIssue
    val proj = projects.findByName(path)?.runtimeOnlyIssue
    return union(global, proj)
  }

  internal fun unusedAnnotationProcessorsIssueFor(path: String): Provider<Behavior> {
    val global = all.unusedAnnotationProcessorsIssue
    val proj = projects.findByName(path)?.unusedAnnotationProcessorsIssue
    return union(global, proj)
  }

  internal fun redundantPluginsIssueFor(path: String): Provider<Behavior> {
    val global = all.redundantPluginsIssue
    val proj = projects.findByName(path)?.redundantPluginsIssue
    return union(global, proj)
  }

  private fun union(global: Issue, project: Issue?): Provider<Behavior> {
    // If there's no project-specific handler, just return the global handler
    return if (project == null) {
      global.behavior()
    } else {
      // If there is a project-specific handler, union it with the global handler. Currently, the
      // behavior with the greatest severity is selected.
      global.behavior().flatMap { a ->
        val allFilter = a.filter
        project.behavior().map { p ->
          val projFilter = p.filter
          val union = allFilter + projFilter

          when (listOf(a, p).maxOrNull()!!) {
            is Fail -> Fail(union)
            is Warn -> Warn(union)
            is Ignore -> Ignore
          }
        }
      }
    }
  }
}

/**
 * ```
 * dependencyAnalysis {
 *   issues {
 *     project(":lib") {
 *       // When true (default is false), will not advise removing unused -ktx dependencies,
 *       // so long as the non-ktx transitive is used.
 *       ignoreKtx(<true|false>)
 *
 *       // Specify severity and exclude rules for all types of dependency violations.
 *       onAny { ... }
 *
 *       // Specify severity and exclude rules for unused dependencies.
 *       onUnusedDependencies { ... }
 *
 *       // Specify severity and exclude rules for undeclared transitive dependencies.
 *       onUsedTransitiveDependencies { ... }
 *
 *       // Specify severity and exclude rules for dependencies declared on the wrong configuration.
 *       onIncorrectConfiguration { ... }
 *
 *       // Specify severity and exclude rules for dependencies that could be compileOnly but are
 *       // otherwise declared.
 *       onCompileOnly { ... }
 *
 *       // Specify severity and exclude rules for dependencies that could be runtimeOnly but are
 *       // otherwise declared.
 *       onRuntimeOnly { ... }
 *
 *       // Specify severity and exclude rules for unused annotation processors.
 *       onUnusedAnnotationProcessors { ... }
 *
 *       // Specify severity and exclude rules for redundant plugins.
 *       onRedundantPlugins { ... }
 *     }
 *   }
 * }
 * ```
 */
open class ProjectIssueHandler @Inject constructor(
  private val name: String,
  objects: ObjectFactory
) : Named {

  override fun getName(): String = name

  internal val anyIssue = objects.newInstance(Issue::class.java)
  internal val unusedDependenciesIssue = objects.newInstance(Issue::class.java)
  internal val usedTransitiveDependenciesIssue = objects.newInstance(Issue::class.java)
  internal val incorrectConfigurationIssue = objects.newInstance(Issue::class.java)
  internal val unusedAnnotationProcessorsIssue = objects.newInstance(Issue::class.java)
  internal val compileOnlyIssue = objects.newInstance(Issue::class.java)
  internal val runtimeOnlyIssue = objects.newInstance(Issue::class.java)
  internal val redundantPluginsIssue = objects.newInstance(Issue::class.java)

  // TODO this should be removed or simply redirect to the DependenciesHandler
  internal val ignoreKtx = objects.property<Boolean>().also {
    it.convention(false)
  }

  fun ignoreKtx(ignore: Boolean) {
    ignoreKtx.set(ignore)
    ignoreKtx.disallowChanges()
  }

  fun onAny(action: Action<Issue>) {
    action.execute(anyIssue)
  }

  fun onUnusedDependencies(action: Action<Issue>) {
    action.execute(unusedDependenciesIssue)
  }

  fun onUsedTransitiveDependencies(action: Action<Issue>) {
    action.execute(usedTransitiveDependenciesIssue)
  }

  fun onIncorrectConfiguration(action: Action<Issue>) {
    action.execute(incorrectConfigurationIssue)
  }

  fun onCompileOnly(action: Action<Issue>) {
    action.execute(compileOnlyIssue)
  }

  fun onRuntimeOnly(action: Action<Issue>) {
    action.execute(runtimeOnlyIssue)
  }

  fun onUnusedAnnotationProcessors(action: Action<Issue>) {
    action.execute(unusedAnnotationProcessorsIssue)
  }

  fun onRedundantPlugins(action: Action<Issue>) {
    action.execute(redundantPluginsIssue)
  }
}

/**
 * ```
 * dependencyAnalysis {
 *   issues {
 *     <all|project(":lib")> {
 *       <onAny|...|onRedundantPlugins> {
 *         // Specify the severity of the violation. Options are "warn", "fail", and "ignore". Default is "warn".
 *         severity("<warn|fail|ignore>")
 *
 *         // Specified excludes are filtered out of the final advice.
 *         exclude(<":lib", "com.some:thing", ...>)
 *       }
 *     }
 *   }
 * }
 * ```
 */
@Suppress("MemberVisibilityCanBePrivate")
open class Issue @Inject constructor(objects: ObjectFactory) {

  private val severity = objects.property(Behavior::class.java).also {
    it.convention(Warn())
  }

  private val excludes: SetProperty<String> = objects.setProperty<String>().also {
    it.convention(emptySet())
  }

  /**
   * Must be one of 'warn', 'fail', or 'ignore'.
   */
  fun severity(value: String) {
    when (value) {
      "warn" -> severity.set(Warn())
      "fail" -> severity.set(Fail())
      "ignore" -> severity.set(Ignore)
      else -> throw GradleException(
        "'value' is not a recognized behavior. Must be one of 'warn', 'fail', or 'ignore'"
      )
    }
    severity.disallowChanges()
  }

  /**
   * All provided elements will be filtered out of the final advice. For example:
   * ```
   * exclude(":lib", "com.some:thing")
   * ```
   * tells the plugin to exclude those dependencies in the final advice.
   */
  fun exclude(vararg ignore: String) {
    excludes.set(ignore.toSet())
    excludes.disallowChanges()
  }

  internal fun behavior(): Provider<Behavior> {
    return excludes.flatMap { filter ->
      severity.map { s ->
        when (s) {
          is Warn -> Warn(filter)
          is Fail -> Fail(filter)
          is Ignore -> Ignore
        }
      }
    }
  }
}

sealed class Behavior(val filter: Set<String> = setOf()) : Serializable, Comparable<Behavior> {
  /**
   * [Fail] > [Ignore] > [Warn].
   */
  override fun compareTo(other: Behavior): Int {
    return when (other) {
      is Fail -> {
        if (this is Fail) 0 else -1
      }
      is Ignore -> {
        when (this) {
          is Fail -> 1
          is Ignore -> 0
          is Warn -> -1
        }
      }
      is Warn -> {
        when (this) {
          is Fail -> 1
          is Ignore -> 1
          is Warn -> 0
        }
      }
    }
  }
}

class Fail(filter: Set<String> = mutableSetOf()) : Behavior(filter)
class Warn(filter: Set<String> = mutableSetOf()) : Behavior(filter)
object Ignore : Behavior()
