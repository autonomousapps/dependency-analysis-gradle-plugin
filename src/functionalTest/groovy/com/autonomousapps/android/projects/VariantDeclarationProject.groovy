// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*

final class VariantDeclarationProject extends AbstractAndroidProject {

  private static final String KOTLINX_COROUTINES_CORE = 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2'

  final GradleProject gradleProject

  VariantDeclarationProject(String agpVersion) {
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
          bs.dependencies(
            new Dependency('debugImplementation', KOTLINX_COROUTINES_CORE),
            new Dependency('releaseImplementation', KOTLINX_COROUTINES_CORE),
          )
        }
      }
      .write()
  }

  private sources = [
    Source
      .kotlin(
        '''
          package com.example.consumer

          import kotlinx.coroutines.flow.Flow
          import kotlinx.coroutines.flow.flowOf

          class Consumer {
            private val values: Flow<Int> = flowOf(1)
          }
        '''
      )
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofRemove(moduleCoordinates(KOTLINX_COROUTINES_CORE), 'debugImplementation'),
    Advice.ofRemove(moduleCoordinates(KOTLINX_COROUTINES_CORE), 'releaseImplementation'),
    Advice.ofAdd(moduleCoordinates(KOTLINX_COROUTINES_CORE), 'implementation'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
  ]
}
