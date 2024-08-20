// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.downgradeKotlinStdlib
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat

final class DataBindingWithExpressionsProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  DataBindingWithExpressionsProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { app ->
        app.withBuildScript { bs ->
          bs.plugins = [Plugins.androidApp, Plugins.kotlinAndroidNoVersion, Plugins.kotlinKaptNoVersion, Plugins.dependencyAnalysisNoVersion]
          bs.android = defaultAndroidAppBlock(true, 'com.example.app')
          bs.dependencies = [
            appcompat("implementation")
          ]
          bs.withGroovy("android.buildFeatures.dataBinding true")
        }
        app.manifest = appManifest('com.example.app')
        app.sources = sources
        app.styles = AndroidStyleRes.DEFAULT
        app.colors = AndroidColorRes.DEFAULT
        app.withFile('src/main/res/layout/main_activity.xml', """\
          <?xml version="1.0" encoding="utf-8"?>
          <layout
            xmlns:android="http://schemas.android.com/apk/res/android">

            <TextView
              android:layout_width="match_parent"
              android:layout_height="match_parent"
              android:text="@{`Text`}" />

          </layout>""".stripIndent()
        )
      }
      .write()
  }

  private sources = [
    new Source(
      SourceType.KOTLIN, 'Library', 'com/example/app',
      """
        package com.example.app
        
        import androidx.appcompat.app.AppCompatActivity
        
        class MainActivity : AppCompatActivity() {
        }
      """.stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':app', downgradeKotlinStdlib())
  ]
}
