// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.autonomousapps.extension

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

/**
 * Configure usage rules.
 *
 * ```
 * dependencyAnalysis {
 *   usage {
 *     analysis { ... }
 *     exclusions { ... }
 *   }
 * }
 * ```
 *
 * @see [UsageAnalysisHandler]
 * @see [UsageExclusionsHandler]
 */
abstract class UsageHandler @Inject constructor(objects: ObjectFactory) {

  internal val analysisHandler: UsageAnalysisHandler = objects.newInstance()
  internal val exclusionsHandler: UsageExclusionsHandler = objects.newInstance()

  fun analysis(action: Action<UsageAnalysisHandler>) {
    action.execute(analysisHandler)
  }

  fun exclusions(action: Action<UsageExclusionsHandler>) {
    action.execute(exclusionsHandler)
  }
}
