@file:Suppress("UnstableApiUsage", "unused")

package com.autonomousapps

import com.autonomousapps.extension.AbiHandler
import com.autonomousapps.extension.DependenciesHandler
import com.autonomousapps.extension.IssueHandler
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
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
 * }
 * ```
 */
@Suppress("MemberVisibilityCanBePrivate")
open class DependencyAnalysisExtension @Inject constructor(objects: ObjectFactory) : AbstractExtension(objects) {

  internal val strictMode: Property<Boolean> = objects.property<Boolean>().convention(true)
  internal val autoApply: Property<Boolean> = objects.property<Boolean>().convention(true)

  override val issueHandler = objects.newInstance(IssueHandler::class)
  internal val abiHandler = objects.newInstance(AbiHandler::class)
  internal val dependenciesHandler = objects.newInstance(DependenciesHandler::class)

  /**
   * If `true`, `buildHealth` will advise the user to declare all transitive dependencies that are
   * being used. If `false`, `buildHealth` will only emit such advice if it would be necessary to
   * have a correctly declared ABI. Otherwise (for implementation dependencies, for example),
   * `buildHealth` now considers it correct to use dependencies that are on the classpath, even if
   * they are not directly declared. Even with `false`, `buildHealth` will not recommend _removing_
   * implementation dependencies, even if they would otherwise be available. Finally, the behavior
   * of `some-project:projectHealth` is unchanged. The analysis required for non-strict mode cannot
   * be done at the project level. Default is `true` (the behavior since v0.1).
   */
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
  fun setDependencyRenamingMap(renamer: Map<String, String>) {
    dependencyRenamingMap.putAll(renamer)
    dependencyRenamingMap.disallowChanges()
  }

  companion object {
    internal const val NAME = "dependencyAnalysis"

    internal fun create(project: Project): DependencyAnalysisExtension = project
      .extensions
      .create(NAME)
  }
}

/** Used for validity check. */
internal fun Project.getExtensionOrNull(): DependencyAnalysisExtension? = rootProject.extensions.findByType()

/** Used after validity check, when it must be non-null. */
internal fun Project.getExtension(): DependencyAnalysisExtension = getExtensionOrNull()!!
