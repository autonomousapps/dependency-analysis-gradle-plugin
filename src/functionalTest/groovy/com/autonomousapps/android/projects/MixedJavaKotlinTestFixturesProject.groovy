// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.android.TestFixturesOptions
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.testImplementation

final class MixedJavaKotlinTestFixturesProject extends AbstractAndroidProject {

  final GradleProject gradleProject

  private final String agpVersion

  MixedJavaKotlinTestFixturesProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder()
      .withRootProject { r ->
        r.gradleProperties += "android.experimental.enableTestFixturesKotlinSupport=true"
      }
      .withAndroidLibProject('consumer') { lib ->
        lib.sources = consumerSources
        lib.manifest = libraryManifest('example.consumer')
        lib.withBuildScript { bs ->
          bs.plugins(androidLib(true))
          bs.android = defaultAndroidLibBlock(true).tap {
            testFixturesOptions = new TestFixturesOptions(true)
          }
          bs.dependencies(testImplementation(':producer').onTestFixtures())
        }
      }
      .withSubproject('producer') { s ->
        s.sources = producerSources
        s.withBuildScript { bs ->
          // java-library is load-bearing. Without that plugin also applied (redundantly?), the test won't fail
          bs.plugins(kotlin + javaTestFixtures + Plugin.javaLibrary)
        }
      }
      .write()
  }

  private List<Source> consumerSources = [
    Source.kotlin(
      '''\
      package example.consumer
      
      import example.producer.fixtures.JavaFixture
      import example.producer.fixtures.KotlinFixture
      
      class ConsumerTest {
        val javaFixture = JavaFixture()
        val kotlinFixture = KotlinFixture()
      }
      '''.stripIndent()
    )
      .withSourceSet('test')
      .build(),
  ]

  private List<Source> producerSources = [
    Source.java(
      '''\
      package example.producer;

      public final class JavaProducer {}
      '''.stripIndent()
    ).build(),
    Source.kotlin(
      '''\
      package example.producer

      class KotlinProducer
      '''.stripIndent()
    ).build(),
    Source.java(
      '''\
      package example.producer.fixtures;

      public final class JavaFixture {}
      '''.stripIndent()
    )
      .withSourceSet('testFixtures')
      .build(),
    Source.kotlin(
      '''\
      package example.producer.fixtures;

      class KotlinFixture
      '''.stripIndent()
    )
      .withSourceSet('testFixtures')
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  Set<ProjectAdvice> expectedBuildHealth() {
    [
      emptyProjectAdviceFor(':consumer'),
      emptyProjectAdviceFor(':producer'),
    ]
  }
}
