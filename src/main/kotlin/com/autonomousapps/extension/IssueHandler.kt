// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.extension

import com.autonomousapps.services.GlobalDslService
import org.gradle.api.Action
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
abstract class IssueHandler @Inject constructor(
  globalDslService: Provider<out GlobalDslService>,
) {

  private val globalDslService = globalDslService.get()

  fun all(action: Action<ProjectIssueHandler>) {
    globalDslService.all(action)
  }

  fun project(projectPath: String, action: Action<ProjectIssueHandler>) {
    globalDslService.project(projectPath, action)
  }

  internal fun shouldAnalyzeSourceSet(sourceSetName: String, projectPath: String): Boolean {
    return globalDslService.shouldAnalyzeSourceSet(sourceSetName, projectPath)
  }

  internal fun anyIssueFor(projectPath: String): List<Provider<Behavior>> {
    return globalDslService.anyIssueFor(projectPath)
  }

  internal fun unusedDependenciesIssueFor(projectPath: String): List<Provider<Behavior>> {
    return globalDslService.unusedDependenciesIssueFor(projectPath)
  }

  internal fun usedTransitiveDependenciesIssueFor(projectPath: String): List<Provider<Behavior>> {
    return globalDslService.usedTransitiveDependenciesIssueFor(projectPath)
  }

  internal fun incorrectConfigurationIssueFor(projectPath: String): List<Provider<Behavior>> {
    return globalDslService.incorrectConfigurationIssueFor(projectPath)
  }

  internal fun compileOnlyIssueFor(projectPath: String): List<Provider<Behavior>> {
    return globalDslService.compileOnlyIssueFor(projectPath)
  }

  internal fun runtimeOnlyIssueFor(projectPath: String): List<Provider<Behavior>> {
    return globalDslService.runtimeOnlyIssueFor(projectPath)
  }

  internal fun unusedAnnotationProcessorsIssueFor(projectPath: String): List<Provider<Behavior>> {
    return globalDslService.unusedAnnotationProcessorsIssueFor(projectPath)
  }

  internal fun redundantPluginsIssueFor(projectPath: String): Provider<Behavior> {
    return globalDslService.redundantPluginsIssueFor(projectPath)
  }

  internal fun moduleStructureIssueFor(projectPath: String): Provider<Behavior> {
    return globalDslService.moduleStructureIssueFor(projectPath)
  }
}
