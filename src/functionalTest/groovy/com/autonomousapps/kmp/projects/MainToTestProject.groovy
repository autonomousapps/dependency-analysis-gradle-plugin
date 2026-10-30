// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.kotlin.KotlinJvmTarget

import static com.autonomousapps.kit.gradle.Dependency.api

final class MainToTestProject extends AbstractKmpProject {

  static final String OKIO = 'com.squareup.okio:okio:3.16.4'

  final GradleProject gradleProject

  MainToTestProject() {
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
                  api(OKIO),
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
            package common.test
            
            import common.main.producer.Producer
            import okio.Buffer
            
            class CommonTest {
              private fun usesOkio(): Buffer {
                val p = Producer()
                return Buffer()
              }
            }
          '''
        )
        .withSourceSet('commonTest')
        .build(),
    ]
  }

  private static List<Source> producerSources() {
    return [
      Source
        .kotlin(
          '''
            package common.main.producer
                        
            class Producer
          '''
        )
        .withSourceSet('commonMain')
        .build(),
    ]
  }

  /*
   * fixDependencies
   */

  final String actualBuildscriptContent() {
    return gradleProject.projectDir('consumer').resolve('build.gradle.kts').text.trim()
  }

  final String expectedBuildScriptContent() {
    return '''\
      plugins {
        id("org.jetbrains.kotlin.multiplatform")
        id("com.autonomousapps.dependency-analysis")
      }
      
      kotlin {
        jvm()
        sourceSets {
          commonMain.dependencies {
          }
          commonTest.dependencies {
            implementation("com.squareup.okio:okio:3.16.4")
            implementation(project(":producer"))
          }
        }
      }'''.stripIndent()
  }
}
