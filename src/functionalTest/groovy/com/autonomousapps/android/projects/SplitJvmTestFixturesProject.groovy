// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.kotlin.Kotlin

import static com.autonomousapps.kit.gradle.Dependency.project

final class SplitJvmTestFixturesProject extends AbstractAndroidProject {

  final GradleProject gradleProject

  SplitJvmTestFixturesProject(String agpVersion) {
    super(agpVersion)
    gradleProject = newGradleProjectBuilder(GradleProject.DslKind.KOTLIN)
      .withRootProject { root ->
        root.gradleProperties += GradleProperties.minimalAndroidProperties()
        root.withBuildScript { buildScript ->
          buildScript.plugins += plugins.androidAppNoApply
          buildScript.withKotlin(
            '''\
            dependencyAnalysis {
              usage {
                analysis {
                  checkSuperClasses(true)
                }
              }
            }'''.stripIndent()
          )
        }
      }
      .withAndroidSubproject('consumer') { consumer ->
        consumer.sources = consumerSources
        consumer.manifest = libraryManifest('com.example.consumer')
        consumer.withBuildScript { buildScript ->
          buildScript.plugins(androidLib(true))
          buildScript.android = defaultAndroidLibBlock(true, 'com.example.consumer').tap {
            defaultConfig = null
          }
          buildScript.kotlin = Kotlin.DEFAULT
          buildScript.dependencies(project('testImplementation', ':producer').onTestFixtures())
        }
      }
      .withSubproject('producer') { producer ->
        producer.sources = producerSources
        producer.withBuildScript { buildScript ->
          buildScript.plugins = kotlin + javaTestFixtures
          buildScript.withKotlin(
            '''\
            sourceSets.named("testFixtures") {
              java.destinationDirectory.set(layout.buildDirectory.dir("testFixtures-classes/java"))
            }
            kotlin.sourceSets.named("testFixtures") {
              kotlin.destinationDirectory.set(layout.buildDirectory.dir("testFixtures-classes/kotlin"))
            }
            '''.stripIndent()
          )
        }
      }
      .write()
  }

  private static final List<Source> consumerSources = [
    Source.kotlin(
      '''\
      package com.example.consumer

      import com.example.producer.KotlinFixture
      import com.example.producer.JavaFixture

      class ConsumerTest {
        val kotlinFixture = KotlinFixture()
        val javaFixture = JavaFixture()
      }
      '''
    )
      .withPath('com.example.consumer', 'ConsumerTest')
      .withSourceSet('test')
      .build(),
  ]

  private static final List<Source> producerSources = [
    Source.kotlin(
      '''\
      package com.example.producer

      class KotlinFixture
      '''
    )
      .withPath('com.example.producer', 'KotlinFixture')
      .withSourceSet('testFixtures')
      .build(),
    Source.java(
      '''\
      package com.example.producer;

      public final class JavaFixture {}
      '''
    )
      .withPath('com.example.producer', 'JavaFixture')
      .withSourceSet('testFixtures')
      .build(),
  ]
}
