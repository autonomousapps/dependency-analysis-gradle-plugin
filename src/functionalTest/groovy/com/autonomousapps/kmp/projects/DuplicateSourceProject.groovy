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

final class DuplicateSourceProject extends AbstractKmpProject {

  private static final VIVO_COMPOSE = 'com.patrykandpatrick.vico:compose:3.3.1'

  final GradleProject gradleProject

  DuplicateSourceProject(agpVersion) {
    super(KOTLIN_VERSION, agpVersion as String)
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder(GradleProject.DslKind.KOTLIN)
      .withRootProject { r ->
        // TODO: delete this block
        r.withSettingsScript { settings ->
          settings.plugins(Plugin.develocity)
          settings.additions = '''\
              develocity {
                server = "https://community.develocity.cloud"
                projectId = "autonomousapps"
                buildScan {
                  publishing.onlyIf { true }
                }
              }
            '''.stripIndent()
        }
        r.withBuildScript { bs ->
          bs.plugins(plugins.androidKmpRootPlugins)
        }
      }
      .withAndroidKmpLibProject('consumer') { s ->
        s.sources = consumerSources()
        s.withBuildScript { bs ->
          bs.plugins = androidKmpLibrary
          bs.kotlinKmp { k ->
            k.jvm()
            k.android { a ->
              a.namespace = 'dagp.test'
              a.compileSdk = 33
              a.minSdk = 24
            }
            k.sourceSets { sourceSets ->
              sourceSets.commonMain { commonMain ->
                commonMain.dependencies(implementation(VIVO_COMPOSE))
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
            package r

            import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
            
            fun producer() = CartesianChartModelProducer()
          '''
        )
        .withPath('r', 'V')
        .withSourceSet('androidMain')
        .build(),
      Source
        .kotlin(
          '''
            package r

            import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
            
            fun producer() = CartesianChartModelProducer()
          '''
        )
        .withPath('r', 'V')
        .withSourceSet('jvmMain')
        .build(),
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofChange(moduleCoordinates(VIVO_COMPOSE), 'commonMainImplementation', 'commonMainApi')
  ]

  final Set<ProjectAdvice> expectedBuildHealth() {
    return [
      projectAdviceForDependencies(':consumer', consumerAdvice),
    ]
  }
}
