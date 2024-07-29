// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.autonomousapps.extension.AbiHandler
import com.autonomousapps.extension.DependenciesHandler
import com.autonomousapps.extension.IssueHandler
import com.autonomousapps.extension.ProjectHandler
import com.autonomousapps.extension.UsagesHandler
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import javax.inject.Inject

/**
 * Summary of top-level DSL config:
 * ```
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
 * }
 * ```
 */
@Suppress("MemberVisibilityCanBePrivate")
open class DependencyAnalysisExtension @Inject constructor(project: Project) : AbstractExtension(project) {

  /** Customize how dependencies are treated. See [DependenciesHandler] for more information. */
  fun structure(action: Action<DependenciesHandler>) {
    action.execute(dependenciesHandler)
  }

  @Deprecated("Use structure", ReplaceWith("structure(action)"))
  fun dependencies(action: Action<DependenciesHandler>) {
    structure(action)
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

  /** Customize project properties. See [ProjectHandler] for more information. */
  fun projectProperties(action: Action<ProjectHandler>) {
    action.execute(projectHandler)
  }

  internal companion object {
    const val NAME = "dependencyAnalysis"

    fun of(project: Project): DependencyAnalysisExtension = project
      .extensions
      .create(NAME, project)
  }
}
