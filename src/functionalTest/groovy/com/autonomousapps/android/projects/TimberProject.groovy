package com.autonomousapps.android.projects

import com.autonomousapps.AdviceHelper
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.timber

final class TimberProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  TimberProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { s ->
        s.styles = AndroidStyleRes.DEFAULT
        s.colors = AndroidColorRes.DEFAULT
        s.manifest = AndroidManifest.app('com.example.MainApplication')
        s.withBuildScript { bs ->
          bs.plugins = [Plugins.androidApp]
          bs.android = defaultAndroidAppBlock(false)
          bs.dependencies = [
            appcompat('implementation'),
            timber('implementation')
          ]
        }
      }
      .write()
  }

  private static Set<Advice> removeTimber = [
    Advice.ofRemove(AdviceHelper.moduleCoordinates('com.jakewharton.timber:timber', '4.7.1'), 'implementation')
  ]

  static ProjectAdvice removeTimberAdvice() {
    projectAdviceForDependencies(':app', removeTimber)
  }
}
