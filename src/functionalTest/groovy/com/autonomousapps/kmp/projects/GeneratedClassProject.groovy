// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class GeneratedClassProject extends AbstractKmpProject {

  static final String TEST_BALLOON_VERSION = '1.2.0'
  static final String TEST_BALLOON_CORE = "de.infix.testBalloon:testBalloon-framework-core:$TEST_BALLOON_VERSION"
  static final String TEST_BALLOON_SHARED = "de.infix.testBalloon:testBalloon-framework-shared:$TEST_BALLOON_VERSION"

  final GradleProject gradleProject

  GeneratedClassProject(agpVersion) {
    super(KOTLIN_VERSION, agpVersion as String)
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withAndroidKmpLibProject('consumer') { s ->
        s.sources = consumerSources()
        s.withBuildScript { bs ->
          bs.plugins = androidKmpLibrary + new Plugin('de.infix.testBalloon', TEST_BALLOON_VERSION)
          bs.kotlinKmp { k ->
            k.androidLibrary { a ->
              a.namespace = 'dagp.consumer'
              a.compileSdk = 33
              a.minSdk = 24
              a.withHostTest()
            }
            k.sourceSets { sourceSets ->
              sourceSets.commonTest { commonTest ->
                commonTest.dependencies(
                  implementation(TEST_BALLOON_CORE),
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

            class CommonMain
          '''
        )
        .withSourceSet('commonMain')
        .build(),
      Source
        .kotlin(
          '''
            package consumer.common.test

            import consumer.common.main.CommonMain
            import de.infix.testBalloon.framework.core.testSuite

            val CommonTest by testSuite {
              test("test") {
                check(CommonMain().toString().isNotEmpty())
              }
            }
          '''
        )
        .withPath('consumer.common.test', 'CommonTest')
        .withSourceSet('commonTest')
        .build(),
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofAdd(moduleCoordinates(TEST_BALLOON_SHARED), 'androidHostTestImplementation'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
  ]
}
