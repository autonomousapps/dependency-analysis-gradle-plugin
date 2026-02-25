// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.api
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class AndroidTargetProject extends AbstractProject {

  private static final String KOTLIN_VERSION = '2.2.21'

  static final String CAFFEINE = 'com.github.ben-manes.caffeine:caffeine:3.2.3'
  static final String KOTLIN_TEST = "org.jetbrains.kotlin:kotlin-test:$KOTLIN_VERSION"
  static final String OKIO = 'com.squareup.okio:okio:3.16.4'

  final GradleProject gradleProject

  AndroidTargetProject(agpVersion) {
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
          bs.kotlin { k ->
            k.androidLibrary { a ->
              a.namespace = 'dagp.test'
              a.compileSdk = 33
              a.minSdk = 24
              a.withHostTest()
            }
            k.sourceSets { sourceSets ->
              sourceSets.commonMain { commonMain ->
                commonMain.dependencies(
                  api('com.squareup.okio:okio-bom:3.16.4').onKmpPlatform(),
                  api('org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2'),
                  implementation(OKIO),
                )
              }
              sourceSets.androidMain { androidMain ->
                androidMain.dependencies(
                  api(CAFFEINE),
                )
              }
              sourceSets.androidHostTest { androidHostTest ->
                androidHostTest.dependencies(
                  implementation('kotlin("test-junit")'),
                  implementation('org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2'),
                  implementation('org.assertj:assertj-core:3.27.7'),
                  implementation('commons-io:commons-io:2.21.0'),
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
            package common.main
            
            import okio.Buffer
            
            abstract class CommonMain {
              abstract fun usesOkio(): Buffer  
            }
          '''
        )
        .withSourceSet('commonMain')
        .build(),
      Source
        .kotlin(
          '''
            package a.main
            
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.launch
            import okio.BufferedSource
            import okio.buffer
            import okio.source
            import java.io.File
            
            abstract class AndroidMain {
              fun usesCoroutines() {
                CoroutineScope(Dispatchers.Main).launch {
                  println("Hi!")
                }
              }
        
              fun usesOkio(): BufferedSource {
                return File(".").source().buffer()
              }
            }
          '''
        )
        .withSourceSet('androidMain')
        .build(),
      Source
        .kotlin(
          '''
            package a.host.test
            
            import kotlinx.coroutines.test.runTest
            import kotlin.test.Test
            import kotlin.test.assertTrue
            
            class HostTestCoroutines {
              @Test fun test() = runTest {
                assertTrue(true)
              }
            }
          '''
        )
        .withSourceSet('androidHostTest')
        .build(),
      Source
        .kotlin(
          '''
            package a.host.test
            
            import org.apache.commons.io.file.Counters
            import org.assertj.core.api.Assertions
            import org.junit.Assert
            import kotlin.test.Test
            
            class HostTestCounters {
              fun test() {
                // commons-io
                val counter = Counters.longCounter()
        
                // org.junit.Assert
                Assert.assertTrue(true)
        
                // org.assertj.core.api.Assertions
                Assertions.assertThat(true).isTrue()
              }
            }
          '''
        )
        .withSourceSet('androidHostTest')
        .build(),
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    // androidMainApi("com.github.ben-manes.caffeine:caffeine:3.2.3")
    Advice.ofRemove(moduleCoordinates(CAFFEINE), 'androidMainApi'),
    // commonMainApi("com.squareup.okio:okio:3.16.4") (was commonMainImplementation)
    Advice.ofChange(moduleCoordinates(OKIO), 'commonMainImplementation', 'commonMainApi'),
    // androidHostTestImplementation("org.jetbrains.kotlin:kotlin-test:2.2.21")
    Advice.ofAdd(moduleCoordinates(KOTLIN_TEST), 'androidHostTestImplementation')
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice)
  ]
}
