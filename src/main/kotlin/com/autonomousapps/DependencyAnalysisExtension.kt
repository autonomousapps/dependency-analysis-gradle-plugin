// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.autonomousapps.extension.*
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * Summary of top-level DSL config:
 * ```
 * // settings.gradle[.kts], or
 * // root build.gradle[.kts]
 * dependencyAnalysis {
 *   // Declare that this subproject is an "application"-type subproject.
 *   app()
 *
 *   // Declare that the plugin should use typesafe project accessors. False by default.
 *   useTypesafeProjectAccessors(true)
 *
 *   // Configure ABI exclusion rules.
 *   abi { ... }
 *
 *   // Configure the severity of issues, and exclusion rules, for potentially the entire project.
 *   issues { ... }
 *
 *   // Configure issue reports.
 *   reporting { ... }
 *
 *   // Configure dependency structure rules (bundles, mapping, etc).
 *   structure { ... }
 *
 *   // Configure usage rules.
 *   usage { ... }
 *
 *   // Configure type usage analysis filtering.
 *   typeUsage { ... }
 * }
 * ```
 */
@Suppress("MemberVisibilityCanBePrivate")
public abstract class DependencyAnalysisExtension @Inject constructor(
  objects: ObjectFactory,
  gradle: Gradle
) : AbstractExtension(objects, gradle) {

  /** Declare that the plugin should use typesafe project accessors. False by default. */
  public fun useTypesafeProjectAccessors(enable: Boolean) {
    useTypesafeProjectAccessors.set(enable)
  }

  /** Customize how the ABI is calculated. See [AbiHandler] for more information. */
  public fun abi(action: Action<AbiHandler>) {
    action.execute(abiHandler)
  }

  /** Customize how "issues" are treated. See [IssueHandler] for more information. */
  public fun issues(action: Action<IssueHandler>) {
    action.execute(issueHandler)
  }

  /** Customize issue reports. See [ReportingHandler] for more information. */
  public fun reporting(action: Action<ReportingHandler>) {
    action.execute(reportingHandler)
  }

  /** Customize how dependencies are treated. See [DependenciesHandler] for more information. */
  public fun structure(action: Action<DependenciesHandler>) {
    action.execute(dependenciesHandler)
  }

  /** Customize how used classes are calculated. See [UsageHandler] for more information. */
  public fun usage(action: Action<UsageHandler>) {
    action.execute(usageHandler)
  }

  /** Customize how used classes are calculated. See [UsageHandler] for more information. */
  @Deprecated(message = "Use 'usage' instead", replaceWith = ReplaceWith("usage"))
  public fun usages(action: Action<UsageHandler>) {
    action.execute(usageHandler)
  }

  /** Customize type usage analysis filtering. See [TypeUsageHandler] for more information. */
  public fun typeUsage(action: Action<TypeUsageHandler>) {
    action.execute(typeUsageHandler)
  }

  internal companion object {
    fun of(project: Project): DependencyAnalysisExtension = project
      .extensions
      .create(NAME, DependencyAnalysisExtension::class.java, project.objects, project.gradle)

    fun of(settings: Settings): DependencyAnalysisExtension = settings
      .extensions
      .create(NAME, DependencyAnalysisExtension::class.java, settings.gradle)
  }
}
