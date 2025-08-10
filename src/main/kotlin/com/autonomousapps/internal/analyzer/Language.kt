// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.analyzer

import org.gradle.api.Action
import org.gradle.api.tasks.util.PatternFilterable

internal enum class Language(val pattern: String) {
  GROOVY("**/*.groovy"),
  JAVA("**/*.java"),
  KOTLIN("**/*.kt"),
  SCALA("**/*.scala"),
  XML("**/*.xml"),
  ;

  companion object {
    fun filterOf(language: Language): Action<in PatternFilterable> = Action {
      Language.values().forEach {
        when (it) {
          language -> include(it.pattern)
          else -> exclude(it.pattern)
        }
      }
    }
  }
}
