// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.AdviceHelper
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class AppleSiliconProject extends AbstractProject {

  static final String sqlite = 'io.github.ganadist.sqlite4java:libsqlite4java-osx-aarch64:1.0.392'

  final GradleProject gradleProject

  AppleSiliconProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('lib') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(
            // should be runtimeOnly
            implementation(sqlite)
          )
        }
      }
      .write()
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return AdviceHelper.actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> libAdvice = [
    Advice.ofChange(moduleCoordinates(sqlite), 'implementation', 'runtimeOnly')
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':lib', libAdvice)
  ]
}
