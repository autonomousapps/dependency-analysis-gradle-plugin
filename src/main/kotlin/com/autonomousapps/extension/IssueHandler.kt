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
 * issues {
 *   all {
 *     onAny {
 *       severity(<'fail'|'warn'|'ignore'>)
 *       exclude('an:external-dep', 'another:external-dep', ':a:project-dep')
 *     }
 *     onUnusedDependencies { ... }
 *     onUsedTransitiveDependencies { ... }
 *     onIncorrectConfiguration { ... }
 *     onRedundantPlugins { ... } // no excludes in this case
 *
 *     ignoreKtx(<true|false>) // default is false
 *   }
 *   project(':lib') {
 *     ...
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
    try {
      projects.create(path) {
        action.execute(this)
      }
    } catch (e: GradleException) {
      throw wrapException(e)
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

          when (listOf(a, p).max()!!) {
            is Fail -> Fail(union)
            is Warn -> Warn(union)
            is Ignore -> Ignore
          }
        }
      }
    }
  }

  @Deprecated("Use all {} instead. Will be removed in 1.0")
  fun ignoreKtx(ignore: Boolean) {
    all.ignoreKtx.set(ignore)
    all.ignoreKtx.disallowChanges()
  }

  @Deprecated("Use all {} instead. Will be removed in 1.0")
  fun onAny(action: Action<Issue>) {
    action.execute(all.anyIssue)
  }

  @Deprecated("Use all {} instead. Will be removed in 1.0")
  fun onUnusedDependencies(action: Action<Issue>) {
    action.execute(all.unusedDependenciesIssue)
  }

  @Deprecated("Use all {} instead. Will be removed in 1.0")
  fun onUsedTransitiveDependencies(action: Action<Issue>) {
    action.execute(all.usedTransitiveDependenciesIssue)
  }

  @Deprecated("Use all {} instead. Will be removed in 1.0")
  fun onIncorrectConfiguration(action: Action<Issue>) {
    action.execute(all.incorrectConfigurationIssue)
  }

  @Deprecated("Use all {} instead. Will be removed in 1.0")
  fun onCompileOnly(action: Action<Issue>) {
    action.execute(all.compileOnlyIssue)
  }

  @Deprecated("Use all {} instead. Will be removed in 1.0")
  fun onUnusedAnnotationProcessors(action: Action<Issue>) {
    action.execute(all.unusedAnnotationProcessorsIssue)
  }
}

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

  fun onUnusedAnnotationProcessors(action: Action<Issue>) {
    action.execute(unusedAnnotationProcessorsIssue)
  }

  fun onRedundantPlugins(action: Action<Issue>) {
    action.execute(redundantPluginsIssue)
  }
}

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

  /*
   * Old and tired.
   */

  @Deprecated("Use `severity()` and `exclude()` instead. Will be removed in 1.0")
  fun fail(vararg ignore: String) {
    @Suppress("DEPRECATION")
    fail(ignore.toSet())
    exclude(*ignore)
  }

  @Deprecated("Use `severity()` and `exclude()` instead. Will be removed in 1.0")
  fun fail(ignore: Iterable<String>) {
    with(severity) {
      set(Fail(ignore.toSet()))
      disallowChanges()
    }
  }

  @Deprecated("Use `severity()` and `exclude()` instead. Will be removed in 1.0")
  fun warn(vararg ignore: String) {
    @Suppress("DEPRECATION")
    warn(ignore.toSet())
    exclude(*ignore)
  }

  @Deprecated("Use `severity()` and `exclude()` instead. Will be removed in 1.0")
  fun warn(ignore: Iterable<String>) {
    with(severity) {
      set(Warn(ignore.toSet()))
      disallowChanges()
    }
  }

  // This takes no arguments because it's implied we're ignoring everything
  @Deprecated("Use `severity()` and `exclude()` instead. Will be removed in 1.0")
  fun ignore() {
    with(severity) {
      set(Ignore)
      disallowChanges()
    }
  }
}

sealed class Behavior(val filter: Set<String> = setOf()) : Serializable, Comparable<Behavior> {
  override fun compareTo(other: Behavior): Int {
    return when (other) {
      is Fail -> {
        if (this is Fail) 0 else -1
      }
      is Warn -> {
        when (this) {
          is Ignore -> -1
          is Warn -> 0
          else -> 1
        }
      }
      is Ignore -> {
        if (this is Ignore) 0 else 1
      }
    }
  }
}

class Fail(filter: Set<String> = mutableSetOf()) : Behavior(filter)
class Warn(filter: Set<String> = mutableSetOf()) : Behavior(filter)
object Ignore : Behavior()
