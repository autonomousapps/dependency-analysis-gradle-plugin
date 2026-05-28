// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation
import static com.autonomousapps.kit.gradle.Dependency.runtimeOnly

final class RuntimeOnlyServiceLoaderProject extends AbstractProject {

  private final mySql = runtimeOnly('com.mysql:mysql-connector-j:8.0.33')
  final GradleProject gradleProject

  RuntimeOnlyServiceLoaderProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { s ->
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(
            implementation(':unused')
          )
        }
      }
      .withSubproject('unused') { s ->
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(mySql)
        }
      }
      .write()
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofRemove(projectCoordinates(':unused'), 'implementation'),
    Advice.ofAdd(moduleCoordinates(mySql), 'runtimeOnly'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
    emptyProjectAdviceFor(':unused'),
  ]
}
