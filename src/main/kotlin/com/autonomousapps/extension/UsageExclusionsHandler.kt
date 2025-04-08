package com.autonomousapps.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.setProperty
import org.intellij.lang.annotations.Language
import javax.inject.Inject

/**
 * Configure usage exclusion rules.
 *
 * ```
 * dependencyAnalysis {
 *   usage {
 *     exclusions {
 *       excludeClasses(".*\\.internal\\..*")
 *     }
 *   }
 * }
 * ```
 */
abstract class UsageExclusionsHandler @Inject constructor(objects: ObjectFactory) {

  internal val classExclusions = objects.setProperty<String>().convention(emptySet())

  @Suppress("unused") // public API
  fun excludeClasses(@Language("RegExp") vararg classRegexes: String) {
    classExclusions.addAll(*classRegexes)
  }
}
