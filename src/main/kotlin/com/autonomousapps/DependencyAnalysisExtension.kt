@file:Suppress("UnstableApiUsage", "unused")

package com.autonomousapps

import com.autonomousapps.extension.AbiHandler
import com.autonomousapps.extension.IssueHandler
import com.autonomousapps.extension.DependenciesHandler
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property

@Suppress("MemberVisibilityCanBePrivate")
open class DependencyAnalysisExtension(objects: ObjectFactory) : AbstractExtension(objects) {

  internal val autoApply: Property<Boolean> = objects.property<Boolean>().also {
    it.convention(true)
  }

  internal val chatty: Property<Boolean> = objects.property<Boolean>().also {
    it.convention(true)
  }

  override val issueHandler = objects.newInstance(IssueHandler::class)
  internal val abiHandler = objects.newInstance(AbiHandler::class)
  internal val dependenciesHandler = objects.newInstance(DependenciesHandler::class)

  @Deprecated("This is now a no-op; you should stop using it. It will be removed in v1.0.0")
  fun setVariants(vararg v: String) {
  }

  /**
   * If `true`, you only apply the plugin to the root project and it will auto-apply to all subprojects. If `false`, you
   * must apply the plugin to each subproject you want to analyze manually. The plugin _must_ also be applied to the
   * root project. Default is `true`.
   */
  fun autoApply(isAutoApply: Boolean) {
    autoApply.set(isAutoApply)
    autoApply.disallowChanges()
  }

  /**
   * If `true`, prints advice to console using `logger.quiet()`. If `false`, prints with `logger.info()`. This should
   * speed up builds, as console logging is relatively inefficient. Default is `true`.
   */
  fun chatty(isChatty: Boolean) {
    chatty.set(isChatty)
    chatty.disallowChanges()
  }

  /**
   * This is now a no-op. Use instead
   * ```
   * dependencies {
   *   bundle("my-group") {
   *     ...
   *   }
   * }
   * ```
   * See the documentation on [DependenciesHandler] for more information.
   */
  @Deprecated("Use dependencies { } instead. Will be removed in 1.0")
  fun setFacadeGroups(vararg facadeGroups: String) {

  }

  /**
   * This is now a no-op. Use instead
   * ```
   * dependencies {
   *   bundle("my-group") {
   *     ...
   *   }
   * }
   * ```
   * See the documentation on [DependenciesHandler] for more information.
   */
  @Deprecated("Use dependencies { bundle(\"my-group\") { ... } } instead. Will be removed in 1.0")
  fun setFacadeGroups(facadeGroups: Iterable<String>) {
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

  internal val dependencyRenamingMap: MapProperty<String, String> =
    objects.mapProperty(String::class.java, String::class.java)

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
}
