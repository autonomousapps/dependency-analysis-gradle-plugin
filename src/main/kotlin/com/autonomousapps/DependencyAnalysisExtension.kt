@file:Suppress("UnstableApiUsage", "unused")

package com.autonomousapps

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import java.io.Serializable
import javax.inject.Inject

open class DependencyAnalysisExtension(objects: ObjectFactory) : AbstractExtension(objects) {

  companion object {
    private const val ANDROID_LIB_VARIANT_DEFAULT = "debug"
    private const val JAVA_LIB_SOURCE_SET_DEFAULT = "main"
  }

  private val defaultVariants = setOf(ANDROID_LIB_VARIANT_DEFAULT, JAVA_LIB_SOURCE_SET_DEFAULT)

  private val theVariants: SetProperty<String> = objects.setProperty<String>().also {
    it.convention(defaultVariants)
  }

  internal val autoApply: Property<Boolean> = objects.property<Boolean>().also {
    it.convention(true)
  }

  internal val chatty: Property<Boolean> = objects.property<Boolean>().also {
    it.convention(true)
  }

  internal val issueHandler: IssueHandler = objects.newInstance(IssueHandler::class.java)

  internal fun getFallbacks() = theVariants.get() + defaultVariants

  fun setVariants(vararg v: String) {
    theVariants.set(v.toSet())
    theVariants.disallowChanges()
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

  fun issues(action: Action<IssueHandler>) {
    action.execute(issueHandler)
  }
}

/**
 * Initial goal:
 * ```
 * issues {
 *   onAny { <fail()|warn()|ignore()> }
 *   onUnusedDependencies { <fail()|warn()|ignore()> }
 *   onUsedTransitiveDependencies { <fail()|warn()|ignore()> }
 *   onIncorrectConfiguration { <fail()|warn()|ignore()> }
 * }
 * ```
 */
open class IssueHandler @Inject constructor(objects: ObjectFactory) {

  internal val anyIssue = objects.newInstance(Issue::class.java)
  internal val unusedDependenciesIssue = objects.newInstance(Issue::class.java)
  internal val usedTransitiveDependenciesIssue = objects.newInstance(Issue::class.java)
  internal val incorrectConfigurationIssue = objects.newInstance(Issue::class.java)

  internal val ignoreKtx = objects.property<Boolean>().also {
    it.convention(false)
  }

  fun ignoreKtx(ignore: Boolean) {
    ignoreKtx.set(ignore)
    ignoreKtx.disallowChanges()
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
}

@Suppress("MemberVisibilityCanBePrivate")
open class Issue @Inject constructor(objects: ObjectFactory) {

  internal val behavior = objects.property(Behavior::class.java).also {
    it.convention(Warn())
  }

  fun fail(vararg ignore: String) {
    fail(ignore.toSet())
  }

  fun fail(ignore: Iterable<String>) {
    with(behavior) {
      set(Fail(ignore.toSet()))
      disallowChanges()
    }
  }

  fun warn(vararg ignore: String) {
    warn(ignore.toSet())
  }

  fun warn(ignore: Iterable<String>) {
    with(behavior) {
      set(Warn(ignore.toSet()))
      disallowChanges()
    }
  }

  // This takes no arguments because it's implied we're ignoring everything
  fun ignore() {
    with(behavior) {
      set(Ignore)
      disallowChanges()
    }
  }
}

sealed class Behavior(val filter: Set<String> = setOf()) : Serializable
class Fail(filter: Set<String> = mutableSetOf()) : Behavior(filter)
class Warn(filter: Set<String> = mutableSetOf()) : Behavior(filter)
object Ignore : Behavior()
