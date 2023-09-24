@file:Suppress("unused")

package com.autonomousapps.extension

import org.gradle.api.InvalidUserDataException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import javax.inject.Inject

/**
 * ```
 * dependencyAnalysis {
 *   issues {
 *     <all|project(":lib")> {
 *       <onAny|...|onRedundantPlugins> {
 *         // Specify the severity of the violation. Options are "warn", "fail", and "ignore". Default is "warn".
 *         severity("<warn|fail|ignore>")
 *
 *         // Specified excludes are filtered out of the final advice.
 *         exclude(<":lib", "com.some:thing", ...>)
 *       }
 *     }
 *   }
 * }
 * ```
 */
@Suppress("MemberVisibilityCanBePrivate")
open class Issue @Inject constructor(
  objects: ObjectFactory
) {

  internal companion object {
    /** Sentinel value indicating that this issue is for _all_ source sets. */
    const val ALL_SOURCE_SETS = "__all"
  }

  internal val sourceSet = objects.property<String>().convention(ALL_SOURCE_SETS)

  private val severity = objects.property<Behavior>().convention(Undefined())
  private val excludes = objects.setProperty<String>().convention(emptySet())

  /** Must be one of 'warn', 'fail', or 'ignore'. */
  fun severity(value: String) {
    when (value) {
      "warn" -> severity.set(Warn())
      "fail" -> severity.set(Fail())
      "ignore" -> severity.set(Ignore())
      else -> throw InvalidUserDataException(
        "'$value' is not a recognized behavior. Must be one of 'warn', 'fail', or 'ignore'"
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
    excludes.addAll(ignore.toSet())
  }

  internal fun behavior(): Provider<Behavior> {
    return excludes.flatMap { filter ->
      severity.map { s ->
        when (s) {
          is Warn -> Warn(filter = filter, sourceSetName = sourceSet.get())
          is Undefined -> Undefined(filter = filter, sourceSetName = sourceSet.get())
          is Fail -> Fail(filter = filter, sourceSetName = sourceSet.get())
          is Ignore -> Ignore(sourceSetName = sourceSet.get())
        }
      }
    }
  }
}
