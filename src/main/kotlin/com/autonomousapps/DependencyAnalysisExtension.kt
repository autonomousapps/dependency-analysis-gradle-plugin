@file:Suppress("UnstableApiUsage", "unused")

package com.autonomousapps

import com.autonomousapps.extension.AbiHandler
import com.autonomousapps.extension.DependenciesHandler
import com.autonomousapps.extension.IssueHandler
import com.autonomousapps.extension.UsagesHandler
import com.autonomousapps.internal.utils.getLogger
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.newInstance
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
open class DependencyAnalysisExtension @Inject constructor(
  project: Project,
  objects: ObjectFactory,
) : AbstractExtension(objects) {

  private val logger = getLogger<DependencyAnalysisExtension>()

  override val issueHandler: IssueHandler = objects.newInstance()
  override val abiHandler: AbiHandler = objects.newInstance()
  internal val usagesHandler: UsagesHandler = objects.newInstance()
  internal val dependenciesHandler: DependenciesHandler = objects.newInstance(project)

  /**
   * Customize how dependencies are treated. See [DependenciesHandler] for more information.
   */
  fun structure(action: Action<DependenciesHandler>) {
    action.execute(dependenciesHandler)
  }

  @Deprecated("Use structure", ReplaceWith("structure(action)"))
  fun dependencies(action: Action<DependenciesHandler>) {
    structure(action)
  }

  /**
   * Customize how the ABI is calculated. See [AbiHandler] for more information.
   */
  fun abi(action: Action<AbiHandler>) {
    action.execute(abiHandler)
  }

  /**
   * Customize how used classes are calculated. See [UsagesHandler] for more information.
   */
  fun usages(action: Action<UsagesHandler>) {
    action.execute(usagesHandler)
  }

  /**
   * Customize how "issues" are treated. See [IssueHandler] for more information.
   */
  fun issues(action: Action<IssueHandler>) {
    action.execute(issueHandler)
  }

  companion object {
    internal const val NAME = "dependencyAnalysis"

    internal fun create(project: Project): DependencyAnalysisExtension = project
      .extensions
      .create(NAME, project)
  }
}

/** Used for validity check. */
internal fun Project.getExtensionOrNull(): DependencyAnalysisExtension? = rootProject.extensions.findByType()

/** Used after validity check, when it must be non-null. */
internal fun Project.getExtension(): DependencyAnalysisExtension = getExtensionOrNull()!!
