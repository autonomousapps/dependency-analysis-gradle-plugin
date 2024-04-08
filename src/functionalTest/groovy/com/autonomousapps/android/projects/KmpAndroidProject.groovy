// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.downgradeKotlinStdlib
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class KmpAndroidProject extends AbstractAndroidProject {

  final GradleProject gradleProject

  KmpAndroidProject(String agpVersion) {
    super(agpVersion)
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { app ->
        app.manifest = AndroidManifest.simpleApp()
        app.sources = sourcesConsumer
        app.withBuildScript { bs ->
          bs.android = defaultAndroidAppBlock()
          bs.plugins = androidAppWithKotlin
          bs.dependencies = [
            // The artifact that is actually used is foundation-android
            implementation('androidx.compose.foundation:foundation:1.6.0-alpha06')
          ]
        }
      }
      .write()
  }

  private sourcesConsumer = [
    new Source(
      SourceType.KOTLIN, "Consumer", "com/example/consumer",
      """\
        package com.example.consumer
        
        import androidx.compose.foundation.isSystemInDarkTheme
        
        class Consumer {
          fun fancyCode() {
            val darkTheme: Boolean = isSystemInDarkTheme()
          }
        }""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':app', downgradeKotlinStdlib()),
  ]
}
