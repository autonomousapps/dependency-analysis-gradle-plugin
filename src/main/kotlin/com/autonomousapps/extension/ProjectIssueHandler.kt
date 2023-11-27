@file:Suppress("unused")

package com.autonomousapps.extension

import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
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
 *       onAny { ... }
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
 *         severity(<'fail'|'warn'|'ignore'>)
 *         exclude('android')
 *       }
 *     }
 *   }
 * }
 * ```
 */
abstract class ProjectIssueHandler @Inject constructor(
  private val projectPath: String,
  objects: ObjectFactory
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

  internal val ignoreKtx = objects.property<Boolean>().also {
    it.convention(false)
  }

  internal val ignoreSourceSets = objects.setProperty<String>()

  /**
   * Set to true to instruct the plugin to not suggest replacing -ktx dependencies with non-ktx dependencies.
   *
   * TODO(2.0) to be removed.
   */
  @Deprecated("Use `dependencyAnalysis { structure { ignoreKtx() } }` instead")
  fun ignoreKtx(ignore: Boolean) {
    ignoreKtx.set(ignore)
    ignoreKtx.disallowChanges()
  }

  fun ignoreSourceSet(vararg ignore: String) {
    ignoreSourceSets.addAll(ignore.toSet())
  }

  /** Specify custom behavior for [sourceSetName]. */
  fun sourceSet(sourceSetName: String, action: Action<ProjectIssueHandler>) {
    sourceSets.maybeCreate(sourceSetName).let { handler ->
      action.execute(handler.project)
    }
  }

  fun onAny(action: Action<Issue>) {
    action.execute(anyIssue)
  }

  fun onUnusedDependencies(action: Action<Issue>) {
    action.execute(unusedDependenciesIssue)
  }

  fun onUsedTransitiveDependencies(action: Action<Issue>) {
    action.execute(usedTransitiveDependenciesIssue)
  }

  fun onIncorrectConfiguration(action: Action<Issue>) {
    action.execute(incorrectConfigurationIssue)
  }

  fun onCompileOnly(action: Action<Issue>) {
    action.execute(compileOnlyIssue)
  }

  fun onRuntimeOnly(action: Action<Issue>) {
    action.execute(runtimeOnlyIssue)
  }

  fun onUnusedAnnotationProcessors(action: Action<Issue>) {
    action.execute(unusedAnnotationProcessorsIssue)
  }

  fun onRedundantPlugins(action: Action<Issue>) {
    action.execute(redundantPluginsIssue)
  }

  fun onModuleStructure(action: Action<Issue>) {
    action.execute(moduleStructureIssue)
  }
}
