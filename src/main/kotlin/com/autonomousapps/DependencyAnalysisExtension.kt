@file:Suppress("UnstableApiUsage", "unused")

package com.autonomousapps

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.setProperty
import java.io.Serializable
import javax.inject.Inject

private const val ANDROID_LIB_VARIANT_DEFAULT = "debug"
internal const val JAVA_LIB_SOURCE_SET_DEFAULT = "main"

open class DependencyAnalysisExtension(objects: ObjectFactory) {

    private val fallbacks: SetProperty<String> = objects.setProperty()
    internal val theVariants: SetProperty<String> = objects.setProperty()

    internal val issueHandler: IssueHandler = objects.newInstance(IssueHandler::class.java)

    init {
        theVariants.convention(listOf(ANDROID_LIB_VARIANT_DEFAULT, JAVA_LIB_SOURCE_SET_DEFAULT))
        fallbacks.set(listOf(ANDROID_LIB_VARIANT_DEFAULT, JAVA_LIB_SOURCE_SET_DEFAULT))
    }

    fun setVariants(vararg v: String) {
        theVariants.set(v.toSet())
        theVariants.disallowChanges()
    }

    fun getFallbacks() = theVariants.get() + fallbacks.get()

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

open class Issue @Inject constructor(objects: ObjectFactory) {

    internal val behavior = objects.property(Behavior::class.java).also {
        it.convention(Warn())
    }

    fun fail(vararg ignore: String) {
        with(behavior) {
            set(Fail(ignore.toSet()))
            disallowChanges()
        }
    }

    fun warn(vararg ignore: String) {
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
