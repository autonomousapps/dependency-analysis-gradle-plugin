package com.autonomousapps.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
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
abstract class ReportingHandler @Inject constructor(private val objects: ObjectFactory) {

  internal val onlyOnFailure: Property<Boolean> = objects.property<Boolean>().convention(false)
  internal val postscript: Property<String> = objects.property<String>().convention("")

  // nb: this intentionally does not have a convention set. If the user does not supply a value, we then check the
  // value of the Gradle property, which itself supplies a default value.
  internal val printBuildHealth: Property<Boolean> = objects.property<Boolean>()

  /**
   * Whether to always include the postscript, or only when the report includes failure-level issues.
   */
  fun onlyOnFailure(onlyOnFailure: Boolean) {
    this.onlyOnFailure.set(onlyOnFailure)
    this.onlyOnFailure.disallowChanges()
  }

  /**
   * A postscript to include in issue reports. Only included when there are issues to report, otherwise ignored.
   */
  fun postscript(postscript: String) {
    this.postscript.set(postscript)
    this.postscript.disallowChanges()
  }

  /**
   * Whether to print the buildHealth report to console.
   *
   * @see [com.autonomousapps.Flags.PRINT_BUILD_HEALTH]
   */
  fun printBuildHealth(printBuildHealth: Boolean) {
    this.printBuildHealth.set(printBuildHealth)
    this.printBuildHealth.disallowChanges()
  }

  internal fun config(): Config {
    val config = objects.newInstance<Config>()
    config.onlyOnFailure.set(onlyOnFailure)
    config.postscript.set(postscript)
    return config
  }

  interface Config {

    @get:Input val onlyOnFailure: Property<Boolean>
    @get:Input val postscript: Property<String>
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
