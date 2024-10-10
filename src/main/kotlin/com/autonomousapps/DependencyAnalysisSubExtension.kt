// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.autonomousapps.extension.AbiHandler
import com.autonomousapps.extension.DependenciesHandler
import com.autonomousapps.extension.ProjectHandler
import com.autonomousapps.extension.ProjectIssueHandler
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import javax.naming.OperationNotSupportedException

/**
 * See also [ProjectIssueHandler]. Note that this differs from [DependencyAnalysisExtension], in that you cannot specify
 * the project being configured, as it is _this_ project being configured.
 *
 * ```
 * dependencyAnalysis {
 *   // Configure the severity of issues, and exclusion rules, for this project.
 *   issues {
 *     ignoreKtx(<true|false>)
 *     onAny { ... }
 *     onUnusedDependencies { ... }
 *     onUsedTransitiveDependencies { ... }
 *     onIncorrectConfiguration { ... }
 *     onCompileOnly { ... }
 *     onUnusedAnnotationProcessors { ... }
 *     onRedundantPlugins { ... }
 *   }
 * }
 * ```
 */
abstract class DependencyAnalysisSubExtension(
  project: Project,
) : AbstractExtension(project.objects, project.gradle) {

  private val path = project.path

  fun issues(action: Action<ProjectIssueHandler>) {
    issueHandler.project(path, action)
  }

  fun abi(action: Action<AbiHandler>) {
    action.execute(abiHandler)
  }

  fun projectProperties(action: Action<ProjectHandler>) {
    action.execute(projectHandler)
  }

  @Suppress("UNUSED_PARAMETER")
  fun structure(action: Action<DependenciesHandler>) {
    throw OperationNotSupportedException("Dependency bundles must be declared in the root project only")
  }

  internal companion object {
    fun of(project: Project): DependencyAnalysisSubExtension {
      return project.extensions.create(NAME, project)
    }
  }
}
