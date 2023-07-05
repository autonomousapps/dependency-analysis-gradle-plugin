@file:Suppress("unused")

package com.autonomousapps.extension

import org.gradle.api.GradleException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
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
open class Issue @Inject constructor(objects: ObjectFactory) {

  private val severity = objects.property(Behavior::class.java).also {
    it.convention(Undefined())
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
    excludes.addAll(ignore.toSet())
  }

  internal fun behavior(): Provider<Behavior> {
    return excludes.flatMap { filter ->
      severity.map { s ->
        when (s) {
          is Warn -> Warn(filter)
          is Undefined -> Undefined(filter)
          is Fail -> Fail(filter)
          is Ignore -> Ignore
        }
      }
    }
  }
}
