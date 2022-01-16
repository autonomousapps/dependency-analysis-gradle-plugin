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
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

/**
 * Summary of top-level DSL config:
 * ```
 * dependencyAnalysis {
 *   // Configure the severity of issues, and exclusion rules, for potentially the entire project.
 *   issues { ... }
 *
 *   // Configure dependency bundles.
 *   dependencies { ... }
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
  objects: ObjectFactory,
  isV1: Boolean
) : AbstractExtension(objects, isV1) {

  private val logger = getLogger<DependencyAnalysisExtension>()

  internal val strictMode: Property<Boolean> = objects.property<Boolean>().convention(true)

  override val issueHandler: IssueHandler = objects.newInstance()
  internal val abiHandler: AbiHandler = objects.newInstance()
  internal val usagesHandler: UsagesHandler = objects.newInstance()
  internal val dependenciesHandler: DependenciesHandler = objects.newInstance()

  /**
   * This option leads to confusing outcomes, as it is impossible in principle for `projectHealth` to respect it. The
   * original default (strict=true) will continue to be the default.
   *
   * @see <a href="https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/518">518</a>
   */
  @Deprecated("Scheduled for removal at some point in the future.")
  fun strictMode(isStrict: Boolean) {
    strictMode.set(isStrict)
    strictMode.disallowChanges()
  }

  /**
   * Customize how dependencies are treated. See [DependenciesHandler] for more information.
   */
  fun dependencies(action: Action<DependenciesHandler>) {
    action.execute(dependenciesHandler)
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

  internal val dependencyRenamingMap: MapProperty<String, String> = objects.mapProperty()

  /**
   * Set a map of literal dependency declarations to semantic aliases. For example:
   * ```
   * dependencyAnalysis {
   *   setDependencyRenamingMap(mapOf("commons-io:commons-io:2.6" to "commonsIo"))
   * }
   * ```
   * This can be useful for projects that have extracted all dependency declarations as semantic
   * maps.
   */
  @Deprecated("Scheduled for removal at some point in the future. If you use this feature, please file an issue at https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/new")
  fun setDependencyRenamingMap(renamer: Map<String, String>) {
    if (!isV1) {
      logger.warn("v2 does not support dependencyRenamingMap")
    }
    dependencyRenamingMap.putAll(renamer)
    dependencyRenamingMap.disallowChanges()
  }

  companion object {
    internal const val NAME = "dependencyAnalysis"

    internal fun create(project: Project, isV1: Boolean): DependencyAnalysisExtension = project
      .extensions
      .create(NAME, isV1)
  }
}

/** Used for validity check. */
internal fun Project.getExtensionOrNull(): DependencyAnalysisExtension? = rootProject.extensions.findByType()

/** Used after validity check, when it must be non-null. */
internal fun Project.getExtension(): DependencyAnalysisExtension = getExtensionOrNull()!!
