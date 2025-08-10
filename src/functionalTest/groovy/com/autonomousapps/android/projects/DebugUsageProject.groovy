// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.ProjectCoordinates

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.testImplementation
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.junit
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.kotlinStdLib

final class DebugUsageProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  private final kotlinStdLib = kotlinStdLib("implementation")

  DebugUsageProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  @SuppressWarnings('DuplicatedCode')
  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('consumer') { p ->
        p.sources = consumerSources
        p.manifest = null
        p.withBuildScript { bs ->
          bs.plugins = androidLibWithKotlin
          bs.android = defaultAndroidLibBlock()
          bs.dependencies(
            kotlinStdLib,
            junit('testImplementation'),
            testImplementation(':producer').onTestFixtures(),
          )
        }
      }
      .withSubproject('producer') { p ->
        p.sources = producerSources
        p.withBuildScript { bs ->
          bs.plugins = kotlin + javaTestFixtures
          bs.dependencies(kotlinStdLib)
        }
      }
      .write()
  }

  private List<Source> consumerSources = [
    Source.kotlin(
      '''\
      package com.example.consumer
      
      import com.example.producer.Producer
      import org.junit.Test
      
      class ExampleUnitTest {
        @Test fun test() {
          val p = Producer()
        }
      }
      '''.stripIndent()
    )
      .withPath('com.example.consumer', 'ExampleUnitTest')
      .withSourceSet('testDebug')
      .build(),
  ]

  private List<Source> producerSources = [
    Source.kotlin(
      '''\
      package com.example.producer
      
      class Producer
      '''.stripIndent()
    )
      .withPath('com.example.producer', 'Producer')
      .withSourceSet('testFixtures')
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private ProjectCoordinates producerTestFixtures = new ProjectCoordinates(
    ':producer',
    GradleVariantIdentification.ofCapabilities('the-project:producer-test-fixtures'),
    ':'
  )

  private final Set<Advice> consumerAdvice = [
    Advice.ofChange(moduleCoordinates('junit:junit:4.13'), 'testImplementation', 'debugTestImplementation'),
    Advice.ofChange(producerTestFixtures, 'testImplementation', 'debugTestImplementation'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
    emptyProjectAdviceFor(':producer'),
  ]
}
