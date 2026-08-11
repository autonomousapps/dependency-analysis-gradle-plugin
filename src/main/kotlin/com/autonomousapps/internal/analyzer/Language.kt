// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.analyzer

import com.autonomousapps.internal.utils.mapToOrderedSet
import org.gradle.api.Action
import org.gradle.api.tasks.util.PatternFilterable

internal enum class Language(val pattern: String, val sourceSetName: String) {
  GROOVY("**/*.groovy", "groovy"),
  JAVA("**/*.java", "java"),
  KOTLIN("**/*.kt", "kotlin"),
  SCALA("**/*.scala", "scala"),
  XML("**/*.xml", "res"),
  ;

  companion object {
    fun filterOf(language: Language): Action<in PatternFilterable> = Action { patternFilterable ->
      entries.forEach {
        when (it) {
          language -> patternFilterable.include(it.pattern)
          else -> patternFilterable.exclude(it.pattern)
        }
      }
    }

    fun languages(): Set<String> {
      return entries
        .filterNot { it == XML }
        .mapToOrderedSet { it.sourceSetName }
    }

    fun languagesPattern(): String {
      return entries
        .filterNot { it == XML }
        .joinToString(prefix = "(?:", postfix = ")", separator = "|") { it.sourceSetName }
    }
  }
}
