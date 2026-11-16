// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class SharedSourceProject extends AbstractKmpProject {

  private static final KOTLINX_COROUTINES_CORE = 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2'

  final GradleProject gradleProject

  SharedSourceProject(agpVersion) {
    super(KOTLIN_VERSION, agpVersion as String)
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder(GradleProject.DslKind.KOTLIN)
      .withRootProject { r ->
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
              a.withDeviceTest()
            }
            k.sourceSets { sourceSets ->
              sourceSets.commonTest { commonTest ->
                commonTest.dependencies(implementation(KOTLINX_COROUTINES_CORE))
                commonTest.srcDirs('src/sharedTest/kotlin')
              }
              sourceSets.named('androidDeviceTest', true) { androidDeviceTest ->
                androidDeviceTest.dependencies(implementation(KOTLINX_COROUTINES_CORE))
                androidDeviceTest.srcDirs('src/sharedTest/kotlin')
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
            package p

            import kotlinx.coroutines.Job
            
            fun shared(): Job = Job()
          '''
        )
        .withPath('p', 'Shared')
        .withSourceSet('sharedTest')
        .build(),
      Source
        .kotlin(
          '''
            package p

            import kotlinx.coroutines.Job
            
            fun other(): Job = Job()
          '''
        )
        .withPath('p', 'Other')
        .withSourceSet('commonTest')
        .build(),
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth() {
    return [emptyProjectAdviceFor(':consumer')]
  }
}
