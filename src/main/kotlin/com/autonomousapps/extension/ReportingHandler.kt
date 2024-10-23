package com.autonomousapps.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/**
 * Customize issue reports.
 *
 * ```
 * dependencyAnalysis {
 *   reporting {
 *     postscript(/* Some text to help out end users who may not be build engineers. */)
 *   }
 * }
 * ```
 */
abstract class ReportingHandler @Inject constructor(objects: ObjectFactory) {

  internal val postscript: Property<String> = objects.property<String>().convention("")

  /**
   * A postscript to include in issue reports. Only included when there are issues to report, otherwise ignored.
   */
  fun postscript(postscript: String) {
    this.postscript.set(postscript)
    this.postscript.disallowChanges()
  }
}
