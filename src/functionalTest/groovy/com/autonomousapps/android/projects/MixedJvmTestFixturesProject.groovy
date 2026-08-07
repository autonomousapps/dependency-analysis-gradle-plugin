// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.kotlin.Kotlin
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class MixedJvmTestFixturesProject extends AbstractAndroidProject {

  final GradleProject gradleProject

  MixedJvmTestFixturesProject(String agpVersion) {
    super(agpVersion)
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder()
      .withAndroidSubproject('consumer') { s ->
        s.sources = consumerSources
        s.manifest = libraryManifest('com.example.consumer')
        s.withBuildScript { bs ->
          bs.plugins(androidLib(true))
          bs.android = defaultAndroidLibBlock(true, 'com.example.consumer')
          bs.kotlin = Kotlin.DEFAULT
          bs.dependencies(
            project('implementation', ':producer'),
            project('testImplementation', ':producer').onTestFixtures(),
          )
        }
      }
      .withSubproject('producer') { s ->
        s.sources = producerSources
        s.withBuildScript { bs ->
          bs.plugins = kotlin + plugins.javaTestFixtures
        }
      }
      .write()
  }

  private final List<Source> consumerSources = [
    Source.kotlin(
      """\
      package com.example.consumer

      import com.example.producer.Producer

      class Consumer {
        fun consume() = Producer().produce()
      }
      """
    )
      .withPath('com.example.consumer', 'Consumer')
      .build(),
    Source.kotlin(
      """\
      package com.example.consumer

      import com.example.producer.FakeProducer

      class ConsumerTest {
        fun consume() = FakeProducer().produce()
      }
      """
    )
      .withPath('com.example.consumer', 'ConsumerTest')
      .withSourceSet('test')
      .build(),
  ]

  private final List<Source> producerSources = [
    Source.kotlin(
      """\
      package com.example.producer

      class Producer {
        fun produce() = Unit
      }
      """
    )
      .withPath('com.example.producer', 'Producer')
      .build(),
    Source.kotlin(
      """\
      package com.example.producer

      class FakeProducer {
        fun produce() = Unit
      }
      """
    )
      .withPath('com.example.producer', 'FakeProducer')
      .withSourceSet('testFixtures')
      .build(),
    new Source(
      SourceType.JAVA,
      'UnusedFixture',
      'com/example/producer',
      """\
      package com.example.producer;

      public final class UnusedFixture {
      }
      """.stripIndent(),
      'testFixtures'
    ),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  static Set<ProjectAdvice> expectedBuildHealth() {
    return [
      emptyProjectAdviceFor(':consumer'),
      emptyProjectAdviceFor(':producer'),
    ]
  }
}
