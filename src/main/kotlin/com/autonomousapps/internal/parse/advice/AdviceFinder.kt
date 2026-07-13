// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.parse.advice

import com.autonomousapps.internal.cash.grammar.kotlindsl.model.DependencyDeclaration
import com.autonomousapps.model.Advice
import com.autonomousapps.model.internal.ProjectType

internal interface AdviceFinder {
  fun findAdvice(dependencyDeclaration: DependencyDeclaration, scope: String = ""): Advice?

  companion object {
    fun of(
      projectType: ProjectType,
      advice: Set<Advice>,
      reversedDependencyMap: (String) -> String,
    ): AdviceFinder {
      return when (projectType) {
        ProjectType.ANDROID, ProjectType.JVM -> StandardAdviceFinder(advice, reversedDependencyMap)
        ProjectType.KMP -> KmpAdviceFinder(advice, reversedDependencyMap)
      }
    }
  }
}
