@file:Suppress("UnstableApiUsage", "unused")

package com.autonomousapps

import com.autonomousapps.extension.AbiHandler
import com.autonomousapps.extension.IssueHandler
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty

@Suppress("MemberVisibilityCanBePrivate")
open class DependencyAnalysisExtension(objects: ObjectFactory) : AbstractExtension(objects) {

  companion object {
    // Facade groups
    private const val KOTLIN_GROUP = "org.jetbrains.kotlin"
  }

  internal val autoApply: Property<Boolean> = objects.property<Boolean>().also {
    it.convention(true)
  }

  internal val chatty: Property<Boolean> = objects.property<Boolean>().also {
    it.convention(true)
  }

  override val issueHandler: IssueHandler = objects.newInstance(IssueHandler::class)

  internal val abiHandler: AbiHandler = objects.newInstance(AbiHandler::class)

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
   * Set of artifact groups known to provide "facades." If unset by user, defaults to Kotlin.
   */
  internal val facadeGroups = objects.setProperty<String>().convention(listOf(KOTLIN_GROUP))

  /**
   * Set list of groups known to provide dependency "facades". When this method is used, it clears
   * the default, meaning that if you _also_ want to include Kotlin in this list, you must add it
   * explicitly.
   */
  fun setFacadeGroups(vararg facadeGroups: String) {
    setFacadeGroups(facadeGroups.toSet())
  }

  /**
   * Set list of groups known to provide dependency "facades". When this method is used, it clears
   * the default, meaning that if you _also_ want to include Kotlin in this list, you must add it
   * explicitly.
   */
  fun setFacadeGroups(facadeGroups: Iterable<String>) {
    this.facadeGroups.addAll(facadeGroups)
    this.facadeGroups.disallowChanges()
  }

  fun abi(action: Action<AbiHandler>) {
    action.execute(abiHandler)
  }

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
