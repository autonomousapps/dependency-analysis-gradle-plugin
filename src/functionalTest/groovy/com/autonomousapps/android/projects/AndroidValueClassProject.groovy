// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.api

final class AndroidValueClassProject extends AbstractAndroidProject {

  private static final String KOTLINX_COROUTINES_CORE = 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2'

  final GradleProject gradleProject

  AndroidValueClassProject(String agpVersion) {
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
          bs.dependencies(api(KOTLINX_COROUTINES_CORE))
        }
      }
      .write()
  }

  private sources = [
    Source
      .kotlin(
        '''
          package com.example.consumer

          import kotlinx.coroutines.channels.ChannelResult

          interface Consumer {
            fun next(): ChannelResult<Int>
          }
        '''
      )
      .withPath('com.example.consumer', 'Consumer')
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
  ]
}
