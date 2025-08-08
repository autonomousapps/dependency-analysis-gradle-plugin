package com.autonomousapps.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/**
 * Configure usage analysis rules.
 *
 * ```
 * dependencyAnalysis {
 *   usage {
 *     analysis {
 *       // When true, do superclass analysis to ensure necessary superclasses are on the classpath. This analysis is
 *       // very expensive.
 *       checkSuperClasses(true) // defaults to false
 *     }
 *   }
 * }
 * ```
 */
public abstract class UsageAnalysisHandler @Inject constructor(objects: ObjectFactory) {

  internal val checkSuperClasses = objects.property<Boolean>().convention(false)

  @Suppress("unused") // public API
  public fun checkSuperClasses(shouldCheck: Boolean) {
    checkSuperClasses.set(shouldCheck)
    checkSuperClasses.disallowChanges()
  }
}
