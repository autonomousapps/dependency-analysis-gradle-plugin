// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Imports
import com.autonomousapps.kit.gradle.kotlin.KotlinJvmTarget
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.api
import static com.autonomousapps.kit.gradle.Dependency.implementation

/**
 * Inspired by https://github.com/slackhq/circuit/blob/b2c06c4891a114645e39c02c1fa852d7db62d827/backstack/build.gradle.kts#L53-L58.
 */
final class HierarchyGroupsProject extends AbstractKmpProject {

  final GradleProject gradleProject

  HierarchyGroupsProject(agpVersion) {
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
      .withSubproject('consumer') { s ->
        s.sources = consumerSources()
        s.withBuildScript { bs ->
          // TODO(tsr): would be nice if these imports were added automatically
          bs.imports = Imports.of(
            'com.android.build.api.withAndroid',
            'org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi'
          )
          bs.plugins = androidKmpLibrary
          bs.kotlinKmp { k ->
            k.jvmTarget = KotlinJvmTarget.default()
            k.android { a ->
              a.namespace = 'com.example'
              a.compileSdk = 37
            }
            k.applyDefaultHierarchyTemplate { template ->
              template.common { common ->
                common.group('commonJvm') { commonJvm ->
                  commonJvm.withJvm()
                  commonJvm.withAndroid()
                }
              }
            }
            k.sourceSets { sourceSets ->
              sourceSets.commonMain { commonMain ->
                commonMain.dependencies(api('org.apache.commons:commons-compress:1.28.0'))
              }
              sourceSets.commonTest { commonTest ->
                commonTest.dependencies(implementation('org.apache.commons:commons-configuration2:2.14.0'))
              }
              sourceSets.named("commonJvmMain", true) { commonJvmMain ->
                commonJvmMain.dependencies(implementation('com.squareup.okhttp3:okhttp:4.6.0'))
              }
            }
          }
        }
      }
      .write()
  }

  private static List<Source> consumerSources() {
    return [
      /* commonJvmMain */
      Source
        .kotlin(
          '''
            package common.main

            import okio.Buffer

            class Foo {
              fun buffer() = Buffer()
            }
          '''
        )
        .withSourceSet('commonJvmMain')
        .build(),

      /* jvmMain */
      Source
        .kotlin(
          '''
            package jvm.main

            import okio.Buffer

            class Bar {
              fun buffer() = Buffer()
            }
          '''
        )
        .withSourceSet('jvmMain')
        .build(),

      /* androidMain */
      Source
        .kotlin(
          '''
            package android.main

            import org.apache.commons.io.output.NullWriter // commons-io

            class More {
              fun nullWriter() = NullWriter()
            }
          '''
        )
        .withSourceSet('androidMain')
        .build(),

      /* commonTest */
      Source
        .kotlin(
          '''
            package common.test

            import org.apache.commons.text.CaseUtils // commons-text

            class Test {
              fun toCamelCase(s: String) = CaseUtils.toCamelCase(s, false)
            }
          '''
        )
        .withSourceSet('commonTest')
        .build(),
    ]
  }

  /*
   * buildHealth
   */

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    // okhttp and okio
    Advice.ofRemove(moduleCoordinates('com.squareup.okhttp3:okhttp:4.6.0'), 'commonJvmMainImplementation'),
    Advice.ofAdd(moduleCoordinates('com.squareup.okio:okio:2.6.0'), 'commonJvmMainApi'),
    // commons-compress and commons-io
    Advice.ofRemove(moduleCoordinates('org.apache.commons:commons-compress:1.28.0'), 'commonMainApi'),
    Advice.ofAdd(moduleCoordinates('commons-io:commons-io:2.20.0'), 'androidMainApi'),
    // commons-configuration2 and commons-text
    Advice.ofRemove(moduleCoordinates('org.apache.commons:commons-configuration2:2.14.0'), 'commonTestImplementation'),
    Advice.ofAdd(moduleCoordinates('org.apache.commons:commons-text:1.15.0'), 'commonTestImplementation'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
  ]

  /*
   * fixDependencies
   */

  final String actualBuildscriptContent() {
    return gradleProject.projectDir('consumer').resolve('build.gradle.kts').text.trim()
  }

  final String expectedBuildScriptContent() {
    return '''\
      import com.android.build.api.withAndroid
      import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
      
      plugins {
        id("org.jetbrains.kotlin.multiplatform")
        id("com.autonomousapps.dependency-analysis")
        id("com.android.kotlin.multiplatform.library")
      }
      
      kotlin {
        jvm()
        androidLibrary {
          namespace = "com.example"
          compileSdk = 37
        }
        @OptIn(ExperimentalKotlinGradlePluginApi::class) applyDefaultHierarchyTemplate {
          common {
            group("commonJvm") {
              withJvm()
              withAndroid()
            }
          }
        }
        sourceSets {
          commonMain.dependencies {
          }
          commonTest.dependencies {
            implementation("org.apache.commons:commons-text:1.15.0")
          }
          getByName("commonJvmMain").dependencies {
            implementation("com.squareup.okhttp3:okhttp:4.6.0")
          }
          commonJvmMain.dependencies {
            api("com.squareup.okio:okio:2.6.0")
          }
          androidMain.dependencies {
            api("commons-io:commons-io:2.20.0")
          }
        }
      }'''.stripIndent()
  }
}
