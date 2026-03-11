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

/**
 * Reproducer for https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1649
 *
 * A KMP project with both android and jvm targets, where a dependency is declared on
 * androidMainImplementation and IS actually used in the androidMain source set. The plugin should
 * not crash with "Change advice cannot be from and to the same configuration".
 */
final class SameConfigKmpProject extends AbstractProject {

  private static final String KOTLIN_VERSION = '2.2.21'

  static final String CAFFEINE = 'com.github.ben-manes.caffeine:caffeine:3.2.3'
  static final String OKIO = 'com.squareup.okio:okio:3.16.4'

  final GradleProject gradleProject

  SameConfigKmpProject(agpVersion) {
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
    // KMP project with both androidLibrary and jvm targets.
    // CAFFEINE is declared on androidMainImplementation and IS used in androidMain.
      .withAndroidKmpLibProject('lib') { s ->
        s.sources = libSources()
        s.withBuildScript { bs ->
          bs.plugins = androidKmpLibrary
          bs.kotlin { k ->
            // jvm target
            k.jvmTarget = KotlinJvmTarget.default()

            // androidLibrary target
            k.androidLibrary { a ->
              a.namespace = 'dagp.lib'
              a.compileSdk = 33
              a.minSdk = 24
            }
            k.sourceSets { sourceSets ->
              sourceSets.commonMain { commonMain ->
                commonMain.dependencies(
                  api('com.squareup.okio:okio-bom:3.16.4').onKmpPlatform(),
                  implementation(OKIO),
                )
              }
              sourceSets.androidMain { androidMain ->
                // This dependency IS used in androidMain source code
                androidMain.dependencies(
                  implementation(CAFFEINE),
                )
              }
            }
          }
        }
      }
      .write()
  }

  private static List<Source> libSources() {
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

            import com.github.benmanes.caffeine.cache.Caffeine

            class AndroidMain {
              fun usesCaffeine() {
                val cache = Caffeine.newBuilder()
                  .maximumSize(100)
                  .build<String, String>()
                cache.put("key", "value")
              }
            }
          '''
        )
        .withSourceSet('androidMain')
        .build(),
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  // CAFFEINE is used on androidMainImplementation and should stay there — no advice for it.
  // OKIO should be changed from commonMainImplementation to commonMainApi.
  private final Set<Advice> libAdvice = [
    Advice.ofChange(moduleCoordinates(OKIO), 'commonMainImplementation', 'commonMainApi'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':lib', libAdvice),
  ]
}