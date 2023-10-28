package com.autonomousapps.android.projects

import com.autonomousapps.AdviceHelper
import com.autonomousapps.kit.*
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.gradle.Dependency.appcompat
import static com.autonomousapps.kit.gradle.Dependency.timber

final class TimberProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  TimberProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { r ->
      r.gradleProperties = GradleProperties.minimalAndroidProperties()
      r.withBuildScript { bs ->
        bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
      }
    }
    builder.withAndroidSubproject('app') { s ->
      s.manifest = AndroidManifest.app('com.example.MainApplication')
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.androidApp]
        bs.android = androidAppBlock(false)
        bs.dependencies = [
          appcompat('implementation'),
          timber('implementation')
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private static Set<Advice> removeTimber = [
    Advice.ofRemove(AdviceHelper.moduleCoordinates('com.jakewharton.timber:timber', '4.7.1'), 'implementation')
  ]

  static ProjectAdvice removeTimberAdvice() {
    projectAdviceForDependencies(':app', removeTimber)
  }
}
