// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.extension

import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/**
 * Configure usage analysis rules.
 *
 * ```
 * dependencyAnalysis {
 *   usage {
 *     analysis {
 *       // When true, does binary compatibility analysis, which can be very expensive in memory terms.
 *       checkBinaryCompatibility(true) // defaults to false
 *
 *       // When true, do superclass analysis to ensure necessary superclasses are on the classpath. This analysis is
 *       // very expensive.
 *       checkSuperClasses(true) // defaults to false
 *     }
 *   }
 * }
 * ```
 */
public abstract class UsageAnalysisHandler @Inject constructor(objects: ObjectFactory) {

  internal val checkBinaryCompat = objects.property(Boolean::class.java).convention(false)
  internal val checkSuperClasses = objects.property(Boolean::class.java).convention(false)

  public fun checkBinaryCompatibility(shouldCheck: Boolean) {
    checkBinaryCompat.set(shouldCheck)
    checkBinaryCompat.disallowChanges()
  }

  public fun checkSuperClasses(shouldCheck: Boolean) {
    checkSuperClasses.set(shouldCheck)
    checkSuperClasses.disallowChanges()
  }
}
