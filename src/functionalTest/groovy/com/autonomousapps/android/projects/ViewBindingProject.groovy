// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat

final class ViewBindingProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  ViewBindingProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { app ->
        app.withBuildScript { bs ->
          bs.plugins = androidApp()
          bs.android = defaultAndroidAppBlock(true, 'com.example.app')
          bs.dependencies(
            appcompat('implementation'),
          )
          bs.withGroovy('android.buildFeatures.viewBinding true')
        }
        app.sources = appSource
        app.manifest = AndroidManifest.app()
        app.styles = AndroidStyleRes.DEFAULT
        app.colors = AndroidColorRes.DEFAULT
      }
      .write()
  }

  private List<Source> appSource = [
    Source.kotlin(
      '''\
        package mutual.aid
        
        import androidx.appcompat.app.AppCompatActivity
        
        class MainActivity : AppCompatActivity() {
        }'''.stripIndent()
    ).build()
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':app')
  ]
}
