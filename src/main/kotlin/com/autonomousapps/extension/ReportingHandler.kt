// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import javax.inject.Inject

/**
 * Customize issue reports.
 *
 * ```
 * dependencyAnalysis {
 *   reporting {
 *     onlyOnFailure(false) // when true, only prints postscript when there are failure-level issues.
 *
 *     postscript(/* Some text to help out end users who may not be build engineers. */)
 *
 *     printBuildHealth(false) // when true, prints buildHealth report to console
 *   }
 * }
 * ```
 */
public abstract class ReportingHandler @Inject constructor(private val objects: ObjectFactory) {

  internal val onlyOnFailure: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
  internal val postscript: Property<String> = objects.property(String::class.java).convention("")

  // nb: this intentionally does not have a convention set. If the user does not supply a value, we then check the
  // value of the Gradle property, which itself supplies a default value.
  internal val printBuildHealth: Property<Boolean> = objects.property(Boolean::class.java)

  /**
   * Whether to always include the postscript, or only when the report includes failure-level issues.
   */
  public fun onlyOnFailure(onlyOnFailure: Boolean) {
    this.onlyOnFailure.set(onlyOnFailure)
    this.onlyOnFailure.disallowChanges()
  }

  /**
   * A postscript to include in issue reports. Only included when there are issues to report, otherwise ignored.
   */
  public fun postscript(postscript: String) {
    this.postscript.set(postscript)
    this.postscript.disallowChanges()
  }

  /**
   * Whether to print the buildHealth report to console.
   *
   * @see [com.autonomousapps.Flags.PRINT_BUILD_HEALTH]
   */
  public fun printBuildHealth(printBuildHealth: Boolean) {
    this.printBuildHealth.set(printBuildHealth)
    this.printBuildHealth.disallowChanges()
  }

  internal fun config(): Config {
    val config = objects.newInstance(Config::class.java)
    config.onlyOnFailure.set(onlyOnFailure)
    config.postscript.set(postscript)
    return config
  }

  public interface Config {
    @get:Input public val onlyOnFailure: Property<Boolean>
    @get:Input public val postscript: Property<String>
  }
}

/**
 * Returns the supplied [postscript][ReportingHandler.Config.postscript], or an empty string, depending on whether we've
 * been configured to print [onlyOnFailure][ReportingHandler.Config.onlyOnFailure] and the actual
 * [failure state][shouldFail] of the advice.
 */
internal fun ReportingHandler.Config.getEffectivePostscript(shouldFail: Boolean): String {
  return if (shouldPrint(shouldFail)) postscript.get() else ""
}

/**
 * Returns true if the [postscript][ReportingHandler.Config.postscript] should be included in output, based on the
 * combination of [onlyOnFailure][ReportingHandler.Config.onlyOnFailure] and the actual [failure state][shouldFail] of
 * the advice.
 */
private fun ReportingHandler.Config.shouldPrint(shouldFail: Boolean): Boolean {
  val onlyOnFailure = onlyOnFailure.get()
  val alwaysPrint = !onlyOnFailure

  return ((onlyOnFailure && shouldFail) || alwaysPrint)
}
