// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.model.Advice
import com.autonomousapps.model.source.KmpSourceKind.Companion.COMMON_MAIN_NAME
import com.autonomousapps.model.source.KmpSourceKind.Companion.COMMON_TEST_NAME

internal object KmpCommonDependencies {

  /**
   * For KMP projects, if the advice is to move the dependency from a commonX source set to a specific target, ignore it
   * for now. If the advice is to move and upgrade, then just upgrade but keep in the same source set.
   */
  fun ensureUnbroken(advice: Advice): Advice? {
    require(advice.isAnyChange()) { "Expected change-advice. Was $advice." }

    val fromConfiguration = advice.fromConfiguration!!
    val toConfiguration = advice.toConfiguration!!
    val fromCommon =
      fromConfiguration.startsWith(COMMON_TEST_NAME) || fromConfiguration.startsWith(COMMON_MAIN_NAME)
    val toCommon =
      toConfiguration.startsWith(COMMON_TEST_NAME) || toConfiguration.startsWith(COMMON_MAIN_NAME)

    return if (fromCommon && !toCommon) {
      if (fromConfiguration.endsWith("Implementation") && toConfiguration.endsWith("Api")) {
        val newConfiguration = fromConfiguration.substringBeforeLast("Implementation") + "Api"
        advice.copy(toConfiguration = newConfiguration)
      } else {
        null
      }
    } else {
      advice
    }
  }
}
