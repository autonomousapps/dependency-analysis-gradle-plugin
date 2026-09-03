// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.kotlin.KotlinJvmTarget
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class JvmDesktopProject extends AbstractProject {

  private static final String KOTLIN_VERSION = '2.4.10'

  final GradleProject gradleProject

  JvmDesktopProject() {
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
          bs.plugins(kmpLibrary)
          bs.kotlinKmp { k ->
            k.jvmTarget = new KotlinJvmTarget("desktop")
            k.sourceSets { sourceSets ->
              sourceSets.named("desktopMain", true) { desktopMain ->
                desktopMain.dependencies(
                  implementation("com.ibm.icu:icu4j:77.1"),
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
            package desktop.main
            
            import com.ibm.icu.text.ListFormatter
            
            fun formatItems(items: List<String>): String = ListFormatter.getInstance().format(items)
          '''
        )
        .withPath('desktop.main', 'example')
        .withSourceSet('desktopMain')
        .build(),
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = []

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice)
  ]
}

