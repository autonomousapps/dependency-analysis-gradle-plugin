@file:Suppress("UnstableApiUsage", "unused")

package com.autonomousapps

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import org.intellij.lang.annotations.Language
import java.io.Serializable
import javax.inject.Inject

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

  internal val issueHandler: IssueHandler = objects.newInstance(IssueHandler::class)

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

/**
 * Initial goal:
 * ```
 * abi {
 *   exclusions {
 *     ignoreSubPackage("internal")
 *     ignoreInternalPackages()
 *     ignoreGeneratedCode()
 *     excludeAnnotations(".*\\.Generated")
 *     excludeClasses(".*\\.internal\\..*")
 *   }
 * }
 * ```
 */
open class AbiHandler @Inject constructor(objects: ObjectFactory) {

  internal val exclusionsHandler: ExclusionsHandler = objects.newInstance(ExclusionsHandler::class)

  fun exclusions(action: Action<ExclusionsHandler>) {
    action.execute(exclusionsHandler)
  }
}

abstract class ExclusionsHandler @Inject constructor(objects: ObjectFactory) {

  internal val classExclusions = objects.setProperty<String>().convention(emptySet())
  internal val annotationExclusions = objects.setProperty<String>().convention(emptySet())
  internal val pathExclusions = objects.setProperty<String>().convention(emptySet())

  fun ignoreInternalPackages() {
    ignoreSubPackage("internal")
  }

  fun ignoreSubPackage(packageFragment: String) {
    excludeClasses("(.*\\.)?$packageFragment(\\..*)?")
  }

  /**
   * Best-effort attempts to ignore generated code by ignoring any bytecode in classes annotated
   * with an annotation ending in `Generated`. It's important to note that the standard
   * `javax.annotation.Generated` (or its JDK9+ successor) does _not_ work with this due to it
   * using `SOURCE` retention. It's recommended to use your own `Generated` annotation.
   */
  fun ignoreGeneratedCode() {
    excludeAnnotations(".*\\.Generated")
  }

  fun excludeClasses(@Language("RegExp") vararg classRegexes: String) {
    classExclusions.addAll(*classRegexes)
  }

  fun excludeAnnotations(@Language("RegExp") vararg annotationRegexes: String) {
    annotationExclusions.addAll(*annotationRegexes)
  }

  // TODO Excluded for now but left as a toe-hold for future use
//  fun excludePaths(@Language("RegExp") vararg pathRegexes: String) {
//    pathExclusions.addAll(*pathRegexes)
//  }
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
  internal val unusedAnnotationProcessorsIssue = objects.newInstance(Issue::class.java)
  internal val compileOnlyIssue = objects.newInstance(Issue::class.java)

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

  fun onUnusedAnnotationProcessors(action: Action<Issue>) {
    action.execute(unusedAnnotationProcessorsIssue)
  }

  fun onCompileOnly(action: Action<Issue>) {
    action.execute(compileOnlyIssue)
  }
}

@Suppress("MemberVisibilityCanBePrivate")
open class Issue @Inject constructor(objects: ObjectFactory) {

  private val severity = objects.property(Behavior::class.java).also {
    it.convention(Warn())
  }

  private val excludes: SetProperty<String> = objects.setProperty<String>().also {
    it.convention(emptySet())
  }

  /**
   * Must be one of 'warn', 'fail', or 'ignore'.
   */
  fun severity(value: String) {
    when (value) {
      "warn" -> severity.set(Warn())
      "fail" -> severity.set(Fail())
      "ignore" -> severity.set(Ignore)
      else -> throw GradleException(
        "'value' is not a recognized behavior. Must be one of 'warn', 'fail', or 'ignore'"
      )
    }
    severity.disallowChanges()
  }

  /**
   * All provided elements will be filtered out of the final advice. For example:
   * ```
   * exclude(":lib", "com.some:thing")
   * ```
   * tells the plugin to exclude those dependencies in the final advice.
   */
  fun exclude(vararg ignore: String) {
    excludes.set(ignore.toSet())
    excludes.disallowChanges()
  }

  internal fun behavior(): Provider<Behavior> {
    return excludes.flatMap { filter ->
      severity.map { s ->
        when (s) {
          is Warn -> Warn(filter)
          is Fail -> Fail(filter)
          is Ignore -> Ignore
        }
      }
    }
  }

  /*
   * Old and tired.
   */

  @Deprecated("Use `behavior()` and `assumeCorrect()` instead. Will be removed in 1.0")
  fun fail(vararg ignore: String) {
    fail(ignore.toSet())
    exclude(*ignore)
  }

  @Deprecated("Use `behavior()` and `assumeCorrect()` instead. Will be removed in 1.0")
  fun fail(ignore: Iterable<String>) {
    with(severity) {
      set(Fail(ignore.toSet()))
      disallowChanges()
    }
  }

  @Deprecated("Use `behavior()` and `assumeCorrect()` instead. Will be removed in 1.0")
  fun warn(vararg ignore: String) {
    warn(ignore.toSet())
    exclude(*ignore)
  }

  @Deprecated("Use `behavior()` and `assumeCorrect()` instead. Will be removed in 1.0")
  fun warn(ignore: Iterable<String>) {
    with(severity) {
      set(Warn(ignore.toSet()))
      disallowChanges()
    }
  }

  // This takes no arguments because it's implied we're ignoring everything
  @Deprecated("Use `behavior()` and `assumeCorrect()` instead. Will be removed in 1.0")
  fun ignore() {
    with(severity) {
      set(Ignore)
      disallowChanges()
    }
  }
}

sealed class Behavior(val filter: Set<String> = setOf()) : Serializable
class Fail(filter: Set<String> = mutableSetOf()) : Behavior(filter)
class Warn(filter: Set<String> = mutableSetOf()) : Behavior(filter)
object Ignore : Behavior()
