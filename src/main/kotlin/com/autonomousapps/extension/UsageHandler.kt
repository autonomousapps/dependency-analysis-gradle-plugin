// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.autonomousapps.extension

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
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
public abstract class UsageHandler @Inject constructor(objects: ObjectFactory) {

  internal val analysisHandler: UsageAnalysisHandler = objects.newInstance(UsageAnalysisHandler::class.java)
  internal val exclusionsHandler: UsageExclusionsHandler = objects.newInstance(UsageExclusionsHandler::class.java)

  public fun analysis(action: Action<UsageAnalysisHandler>) {
    action.execute(analysisHandler)
  }

  public fun exclusions(action: Action<UsageExclusionsHandler>) {
    action.execute(exclusionsHandler)
  }
}
