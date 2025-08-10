// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class AndroidTestSmokeProject extends AbstractAndroidProject {

  /** Unused. Brings along Okio, which is used. */
  private static final okHttp = okHttp('implementation')

  final GradleProject gradleProject
  private final String agpVersion

  AndroidTestSmokeProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { subproject ->
        subproject.sources = appSources()
        subproject.styles = AndroidStyleRes.DEFAULT
        subproject.colors = AndroidColorRes.DEFAULT

        subproject.withBuildScript { buildScript ->
          buildScript.plugins(Plugins.androidApp, Plugins.kotlinAndroidNoVersion, Plugins.dependencyAnalysisNoVersion)
          buildScript.android = defaultAndroidAppBlock()
          buildScript.dependencies(
            kotlinStdLib('implementation'),
            appcompat('implementation'),
            junit('implementation'),
          )
        }
      }
      .withAndroidSubproject('benchmark') { test ->
        test.sources = androidBenchmarkSources
        test.manifest = AndroidManifest.defaultLib('com.example.test')

        test.withBuildScript { buildScript ->
          buildScript.plugins(Plugins.androidTest, Plugins.kotlinAndroidNoVersion, Plugins.dependencyAnalysisNoVersion)
          buildScript.android = defaultAndroidTestBlock(':app', true)
          buildScript.dependencies(okHttp)
        }
      }
      .write()
  }

  private static List<Source> appSources() {
    return [
      Source.kotlin(
        '''\
          package com.example
          
          class App {
            fun magic() = 42
          }
        '''
      )
        .withPath('com.example', 'App')
        .build()
    ]
  }

  private androidBenchmarkSources = [
    Source.kotlin(
      '''\
        package com.example
      
        import okio.Buffer
      
        class Lib {
          val buffer = Buffer()
        }'''.stripIndent()
    )
      .withPath('com.example', 'Lib')
      .build()
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private static Set<Advice> appAdvice() {
    return [Advice.ofRemove(moduleCoordinates('junit:junit', '4.13'), 'implementation')]
  }

  private static Set<Advice> benchmarkAdvice() {
    return [
      Advice.ofRemove(moduleCoordinates(okHttp), okHttp.configuration),
      Advice.ofAdd(moduleCoordinates('com.squareup.okio:okio', '2.6.0'), 'implementation'),
    ]
  }

  Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':app', appAdvice()),
    projectAdviceForDependencies(':benchmark', benchmarkAdvice()),
  ]
}
