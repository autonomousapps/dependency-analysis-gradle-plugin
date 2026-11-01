// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.api

final class ValueClassProject extends AbstractKmpProject {

  final GradleProject gradleProject

  ValueClassProject(agpVersion) {
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
                  api(':producer'),
                )
              }
            }
          }
        }
      }
      .withAndroidKmpLibProject('producer') { s ->
        s.sources = producerSources()
        s.withBuildScript { bs ->
          bs.plugins = androidKmpLibrary
          bs.kotlinKmp { k ->
            k.androidLibrary { a ->
              a.namespace = 'dagp.producer'
              a.compileSdk = 33
              a.minSdk = 24
              a.withHostTest()
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

            import producer.common.main.Id

            class CommonMain(val id: Id)
          '''
        )
        .withSourceSet('commonMain')
        .build(),
    ]
  }

  private static List<Source> producerSources() {
    return [
      Source
        .kotlin(
          '''
            package producer.common.main

            import kotlin.jvm.JvmInline

            @JvmInline
            value class Id(val value: String)
          '''
        )
        .withSourceSet('commonMain')
        .build(),
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
    emptyProjectAdviceFor(':producer'),
  ]
}
