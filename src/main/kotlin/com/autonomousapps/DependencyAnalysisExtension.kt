// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.autonomousapps.extension.*
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.create
import javax.inject.Inject

/**
 * Summary of top-level DSL config:
 * ```
 * // settings.gradle[.kts], or
 * // root build.gradle[.kts]
 * dependencyAnalysis {
 *   // Configure the severity of issues, and exclusion rules, for potentially the entire project.
 *   issues { ... }
 *
 *   // Configure dependency structure rules (bundles, mapping, etc).
 *   structure { ... }
 *
 *   // Configure ABI exclusion rules.
 *   abi { ... }
 *
 *   // Configure usages exclusion rules.
 *   usages { ... }
 *
 *   // Configure issue reports.
 *   reporting { ... }
 * }
 * ```
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class DependencyAnalysisExtension @Inject constructor(
  objects: ObjectFactory,
  gradle: Gradle
) : AbstractExtension(objects, gradle) {

  override var useTypesafeProjectAccessors: Boolean = false

  /** Customize how dependencies are treated. See [DependenciesHandler] for more information. */
  fun structure(action: Action<DependenciesHandler>) {
    action.execute(dependenciesHandler)
  }

  /** Customize how the ABI is calculated. See [AbiHandler] for more information. */
  fun abi(action: Action<AbiHandler>) {
    action.execute(abiHandler)
  }

  /** Customize how used classes are calculated. See [UsagesHandler] for more information. */
  fun usages(action: Action<UsagesHandler>) {
    action.execute(usagesHandler)
  }

  /** Customize how "issues" are treated. See [IssueHandler] for more information. */
  fun issues(action: Action<IssueHandler>) {
    action.execute(issueHandler)
  }

  /** Customize issue reports. See [ReportingHandler] for more information. */
  fun reporting(action: Action<ReportingHandler>) {
    action.execute(reportingHandler)
  }

  internal companion object {
    fun of(project: Project): DependencyAnalysisExtension = project
      .extensions
      .create(NAME, project.objects, project.gradle)

    fun of(settings: Settings): DependencyAnalysisExtension = settings
      .extensions
      .create(NAME, settings.gradle)
  }
}
