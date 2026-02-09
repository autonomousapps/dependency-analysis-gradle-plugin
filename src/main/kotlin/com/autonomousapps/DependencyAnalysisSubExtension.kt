// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.autonomousapps.extension.AbiHandler
import com.autonomousapps.extension.DependenciesHandler
import com.autonomousapps.extension.ProjectIssueHandler
import com.autonomousapps.extension.TypeUsageHandler
import org.gradle.api.Action
import org.gradle.api.Project
import javax.naming.OperationNotSupportedException

/**
 * See also [ProjectIssueHandler]. Note that this differs from [DependencyAnalysisExtension], in that you cannot specify
 * the project being configured, as it is _this_ project being configured.
 *
 * ```
 * dependencyAnalysis {
 *   // Declare that this subproject is an "application"-type subproject.
 *   app()
 *
 *   // Configure ABI analysis.
 *   abi { ... }
 *
 *   // Configure the severity of issues, and exclusion rules, for this project.
 *   issues {
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
public abstract class DependencyAnalysisSubExtension(
  project: Project,
) : AbstractExtension(project.objects, project.gradle) {

  private val path = project.path

  public fun abi(action: Action<AbiHandler>) {
    action.execute(abiHandler)
  }

  public fun issues(action: Action<ProjectIssueHandler>) {
    issueHandler.project(path, action)
  }

  @Suppress("UNUSED_PARAMETER")
  public fun structure(action: Action<DependenciesHandler>) {
    throw OperationNotSupportedException("Dependency bundles must be declared in the root project only")
  }

  /** Customize type usage analysis filtering. See [TypeUsageHandler] for more information. */
  public fun typeUsage(action: Action<TypeUsageHandler>) {
    action.execute(typeUsageHandler)
  }

  internal companion object {
    fun of(project: Project): DependencyAnalysisSubExtension {
      return project.extensions.create(NAME, DependencyAnalysisSubExtension::class.java, project)
    }
  }
}
