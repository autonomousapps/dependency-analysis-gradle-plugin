// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.api

final class BundleParentProject extends AbstractKmpProject {

  static final String KOTLINX_COROUTINES_CORE = 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2'

  final GradleProject gradleProject

  BundleParentProject(agpVersion) {
    super(KOTLIN_VERSION, agpVersion as String)
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder(GradleProject.DslKind.KOTLIN)
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.plugins(
            plugins.dependencyAnalysis,
            plugins.kotlinMultiplatformNoApply,
            plugins.androidKmpLibNoApply,
          )
        }
      }
      .withAndroidKmpLibProject('consumer') { s ->
        s.sources = consumerSources()
        s.withBuildScript { bs ->
          bs.plugins = androidKmpLibrary
          bs.kotlinKmp { k ->
            k.androidLibrary { a ->
              a.namespace = 'dagp.consumer'
              a.compileSdk = 33
              a.minSdk = 24
              a.withHostTest()
            }
            k.sourceSets { sourceSets ->
              sourceSets.commonMain { commonMain ->
                commonMain.dependencies(
                  api(KOTLINX_COROUTINES_CORE),
                )
              }
            }
          }
        }
      }
      .write()
  }

  private static List<Source> consumerSources() {
    return [
      Source
        .kotlin(
          '''
            package consumer.common.main

            import kotlinx.coroutines.flow.Flow
            import kotlinx.coroutines.flow.flowOf

            class CommonMain {
              fun values(): Flow<Int> = flowOf(1)
            }
          '''
        )
        .withSourceSet('commonMain')
        .build(),
      Source
        .kotlin(
          '''
            package consumer.common.test

            import kotlinx.coroutines.flow.flowOf

            class CommonTest {
              fun values() = flowOf(1)
            }
          '''
        )
        .withSourceSet('commonTest')
        .build(),
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
  ]
}
