package com.autonomousapps.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/**
 * Customize issue reports.
 *
 * ```
 * dependencyAnalysis {
 *   reporting {
 *     onlyOnFailure(false) // when true, only prints postscript when there are failure-level issues.
 *     postscript(/* Some text to help out end users who may not be build engineers. */)
 *   }
 * }
 * ```
 */
abstract class ReportingHandler @Inject constructor(objects: ObjectFactory) {

  internal val onlyOnFailure: Property<Boolean> = objects.property<Boolean>().convention(false)
  internal val postscript: Property<String> = objects.property<String>().convention("")

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

  internal fun config() = Config(
    onlyOnFailure = onlyOnFailure,
    postscript = postscript,
  )

  class Config(
    @get:Input val onlyOnFailure: Property<Boolean>,
    @get:Input val postscript: Property<String>,
  ) {

    /**
     * Returns the supplied [postscript], or an empty string, depending on whether we've been configured to print
     * [onlyOnFailure] and the actual [failure state][shouldFail] of the advice.
     */
    internal fun getEffectivePostscript(shouldFail: Boolean): String {
      return if (shouldPrint(shouldFail)) postscript.get() else ""
    }

    /**
     * Returns true if the [postscript] should be included in output, based on the combination of [onlyOnFailure] and
     * the actual [failure state][shouldFail] of the advice.
     */
    private fun shouldPrint(shouldFail: Boolean): Boolean {
      val onlyOnFailure = onlyOnFailure.get()
      val alwaysPrint = !onlyOnFailure

      return ((onlyOnFailure && shouldFail) || alwaysPrint)
    }
  }
}
