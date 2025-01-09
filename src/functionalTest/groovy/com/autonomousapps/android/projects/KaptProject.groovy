// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.dependencies.Plugins

import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class KaptProject extends AbstractAndroidProject {

  final String agpVersion
  final GradleProject gradleProject

  KaptProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withRootProject { root ->
        root.withBuildScript { bs ->
          bs.withGroovy("""
          dependencyAnalysis {
            issues {
              all {
                onRedundantPlugins {
                  severity('fail')
                  exclude('kotlin-kapt')
                }
              }
            }
          }
        """)
        }
      }
      .withAndroidSubproject('lib') { a ->
        a.sources = sources
        a.manifest = libraryManifest()
        a.withBuildScript { bs ->
          bs.plugins = [Plugins.androidLib, Plugins.kotlinAndroidNoVersion, Plugins.kotlinKaptNoVersion, Plugins.dependencyAnalysisNoVersion]
          bs.android = defaultAndroidLibBlock(true)
          bs.dependencies = dependencies
        }
      }.write()
  }

  private static final List<Source> sources = [
    new Source(
      SourceType.KOTLIN, 'Main', 'com/example',
      """\
        package com.example
        
        class Main
       """.stripIndent()
    )
  ]

  private List<Dependency> dependencies = [
    appcompat("implementation"),
    dagger("androidTestImplementation"),
    daggerCompiler("kaptAndroidTest")
  ]
}
