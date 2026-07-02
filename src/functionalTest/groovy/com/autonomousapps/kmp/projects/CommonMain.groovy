// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.kotlin.KotlinJvmTarget
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.api
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class CommonMain extends AbstractProject {

  private static final String KOTLIN_VERSION = '2.2.21'

  final GradleProject gradleProject

  CommonMain() {
    super(KOTLIN_VERSION)
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder(GradleProject.DslKind.KOTLIN)
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.plugins(plugins.dependencyAnalysis, plugins.kotlinMultiplatformNoApply)
        }
      }
      .withSubproject('consumer') { s ->
        s.sources = consumerSources()
        s.withBuildScript { bs ->
          bs.plugins = kmpLibrary
          bs.kotlinKmp { k ->
            k.jvmTarget = KotlinJvmTarget.default()
            k.sourceSets { sourceSets ->
              sourceSets.commonMain { commonMain ->
                commonMain.dependencies(
                  api(':producer'),
                )
              }
              sourceSets.commonTest { commonTest ->
                commonTest.dependencies(
                  implementation('kotlin("test")'),
                )
              }
              sourceSets.jvmTest { jvmTest ->
                jvmTest.dependencies(
                  implementation('kotlin("test-junit")'),
                )
              }
            }
          }
        }
      }
      .withSubproject('producer') { s ->
        s.sources = producerSources()
        s.withBuildScript { bs ->
          bs.plugins = kmpLibrary
          bs.kotlinKmp { k ->
            k.jvmTarget = KotlinJvmTarget.default()
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
            
            import producer.common.main.Producer
            
            class CommonMain(val producer: Producer)
          '''
        )
        .withSourceSet('commonMain')
        .build(),
      Source
        .kotlin(
          '''
            package consumer.common.test
            
            import kotlin.test.Test
            import kotlin.test.assertTrue
            import producer.common.main.Producer
            
            class CommonTest {
              @Test fun test() {
                val producer = Producer()
                assertTrue(true)
              }
            }
          '''
        )
        .withSourceSet('commonTest')
        .build(),
    ]
  }

  protected static List<Source> producerSources() {
    return [
      Source
        .kotlin(
          '''
            package producer.common.main
                        
            class Producer
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
