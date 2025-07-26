// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("unused")

package com.autonomousapps.extension

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.setProperty
import javax.inject.Inject

/**
 * ```
 * dependencyAnalysis {
 *   issues {
 *     project(":lib") {
 *       // One or more source sets (by name) to ignore in dependency analysis.
 *       ignoreSourceSet(...)
 *
 *       // Specify severity and exclude rules for all types of dependency violations.
 *       onAny {
 *         severity(<"fail"|"warn"|"ignore">)
 *
 *         // using version catalog accessors
 *         exclude(libs.guava, ...)
 *
 *         // using basic string coordinates
 *         exclude("com.google.guava:guava", ...)
 *       }
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
 *
 *       // Specify severity and exclude rules for module structure advice.
 *       onModuleStructure {
 *         severity(<"fail"|"warn"|"ignore">)
 *         exclude("android")
 *       }
 *
 *       onDuplicateClassWarnings {
 *          severity(<"fail"|"warn"|"ignore">)
 *
 *          // Fully-qualified class reference to exclude, slash- or dot-delimited
 *          exclude("org/jetbrains/annotations/NotNull", "org.jetbrains.annotations.Nullable")
 *       }
 *     }
 *   }
 * }
 * ```
 */
public abstract class ProjectIssueHandler @Inject constructor(
  private val projectPath: String,
  objects: ObjectFactory,
) : Named {

  override fun getName(): String = projectPath

  internal val sourceSets = objects.domainObjectContainer(
    SourceSetsHandler::class.java,
    SourceSetsHandler.Factory(projectPath, objects)
  )

  internal val anyIssue = objects.newInstance<Issue>()
  internal val unusedDependenciesIssue = objects.newInstance<Issue>()
  internal val usedTransitiveDependenciesIssue = objects.newInstance<Issue>()
  internal val incorrectConfigurationIssue = objects.newInstance<Issue>()
  internal val unusedAnnotationProcessorsIssue = objects.newInstance<Issue>()
  internal val compileOnlyIssue = objects.newInstance<Issue>()
  internal val runtimeOnlyIssue = objects.newInstance<Issue>()
  internal val redundantPluginsIssue = objects.newInstance<Issue>()
  internal val moduleStructureIssue = objects.newInstance<Issue>()
  internal val duplicateClassWarningsIssue = objects.newInstance<Issue>()

  internal val ignoreSourceSets = objects.setProperty<String>()

  public fun ignoreSourceSet(vararg ignore: String) {
    ignoreSourceSets.addAll(ignore.toSet())
  }

  /** Specify custom behavior for [sourceSetName]. */
  public fun sourceSet(sourceSetName: String, action: Action<ProjectIssueHandler>) {
    sourceSets.maybeCreate(sourceSetName).let { handler ->
      action.execute(handler.project)
    }
  }

  public fun onAny(action: Action<Issue>) {
    action.execute(anyIssue)
  }

  public fun onUnusedDependencies(action: Action<Issue>) {
    action.execute(unusedDependenciesIssue)
  }

  public fun onUsedTransitiveDependencies(action: Action<Issue>) {
    action.execute(usedTransitiveDependenciesIssue)
  }

  public fun onIncorrectConfiguration(action: Action<Issue>) {
    action.execute(incorrectConfigurationIssue)
  }

  public fun onCompileOnly(action: Action<Issue>) {
    action.execute(compileOnlyIssue)
  }

  public fun onRuntimeOnly(action: Action<Issue>) {
    action.execute(runtimeOnlyIssue)
  }

  public fun onUnusedAnnotationProcessors(action: Action<Issue>) {
    action.execute(unusedAnnotationProcessorsIssue)
  }

  public fun onRedundantPlugins(action: Action<Issue>) {
    action.execute(redundantPluginsIssue)
  }

  public fun onModuleStructure(action: Action<Issue>) {
    action.execute(moduleStructureIssue)
  }

  public fun onDuplicateClassWarnings(action: Action<Issue>) {
    action.execute(duplicateClassWarningsIssue)
  }
}
