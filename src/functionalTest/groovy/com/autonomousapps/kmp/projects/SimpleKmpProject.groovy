// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.kotlin.KotlinJvmTarget
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.api
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class SimpleKmpProject extends AbstractProject {

  private static final KOTLIN_VERSION = '2.2.21'

  final GradleProject gradleProject

  SimpleKmpProject() {
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
          bs.kotlin { k ->
            k.jvmTarget = KotlinJvmTarget.default()
            k.sourceSets { sourceSets ->
              sourceSets.commonMain { commonMain ->
                commonMain.dependencies(
                  api("com.squareup.okio:okio-bom:3.16.4").onKmpPlatform(),
                  api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2"),
                  implementation("com.squareup.okio:okio:3.16.4"),
                )
              }
              sourceSets.commonTest { commonTest ->
                commonTest.dependencies(
                  implementation("kotlin(\"test\")"),
                  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2"),
                )
              }
              sourceSets.jvmMain { jvmMain ->
                jvmMain.dependencies(
                  api("com.github.ben-manes.caffeine:caffeine:3.2.3"),
                )
              }
              sourceSets.jvmTest { jvmTest ->
                jvmTest.dependencies(
                  implementation("kotlin(\"test-junit\")"),
                  implementation("org.assertj:assertj-core:3.27.7"),
                  implementation("commons-io:commons-io:2.21.0"),
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
            package common.test
            
            import kotlinx.coroutines.test.runTest
            import kotlin.test.Test
            import kotlin.test.assertTrue
            
            class CommonTest {
              @Test fun test() = runTest {
                assertTrue(true)
              }
            }
          '''
        )
        .withSourceSet('commonTest')
        .build(),
      Source
        .kotlin(
          '''
            package jvm.main
            
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.launch
            import okio.BufferedSource
            import okio.buffer
            import okio.source
            import java.io.File
            
            abstract class JvmMain {
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
        .withSourceSet('jvmMain')
        .build(),
      Source
        .kotlin(
          '''
            package jvm.test
            
            import org.apache.commons.io.file.Counters
            import org.assertj.core.api.Assertions
            import org.junit.Assert
            import kotlin.test.Test
            
            class JvmTest {
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
        .withSourceSet('jvmTest')
        .build(),
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    // jvmMainApi("com.github.ben-manes.caffeine:caffeine:3.2.3")
    Advice.ofRemove(moduleCoordinates('com.github.ben-manes.caffeine:caffeine:3.2.3'), 'jvmMainApi'),
    // commonMainApi("com.squareup.okio:okio:3.16.4") (was commonMainImplementation)
    Advice.ofChange(moduleCoordinates('com.squareup.okio:okio:3.16.4'), 'commonMainImplementation', 'commonMainApi'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice)
  ]
}
