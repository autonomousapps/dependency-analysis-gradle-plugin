// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.api

final class SupertypeTypeArgumentProject extends AbstractAndroidProject {
  
  final GradleProject gradleProject

  SupertypeTypeArgumentProject(String agpVersion) {
    super(agpVersion)
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder()
      .withAndroidLibProject('consumer') { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins(androidLib())
          bs.android = defaultAndroidLibBlock('com.example.consumer')
          bs.dependencies(api('org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2'))
        }
      }
      .write()
  }

  private sources = [
    Source
      .kotlin(
        '''
          package com.example.consumer

          import kotlinx.coroutines.Deferred

          interface Consumer : Comparable<Deferred<List<String>>>
        '''
      )
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
  ]
}
