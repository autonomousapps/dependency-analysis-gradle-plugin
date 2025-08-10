// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.kotlinStdLib

final class LeakCanaryProject extends AbstractAndroidProject {

  static final String LEAK_CANARY_VERSION = '2.14'

  private final String agpVersion
  final GradleProject gradleProject

  LeakCanaryProject(String agpVersion) {
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
            new Dependency('debugImplementation', "com.squareup.leakcanary:leakcanary-android:$LEAK_CANARY_VERSION"),
          )
        }
      }
      .write()
  }

  private static List<Source> appSources() {
    return [
      Source.kotlin(
        '''\
          package com.autonomousapps.test
          
          import androidx.appcompat.app.AppCompatActivity
          
          class MainActivity : AppCompatActivity()
        '''
      )
        .withPath('com.autonomousapps.test', 'App') // TODO(tsr) delete line
        .build()
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private static Set<Advice> appAdvice() {
    def leakcanaryAndroid = moduleCoordinates('com.squareup.leakcanary:leakcanary-android', LEAK_CANARY_VERSION)
    return [
      Advice.ofChange(leakcanaryAndroid, 'debugImplementation', 'debugRuntimeOnly'),
    ]
  }

  Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':app', appAdvice()),
  ]
}
