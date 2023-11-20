package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.appcompat
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.rxlint

final class LintJarProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  private final rxlint = rxlint('implementation')

  LintJarProject(String agpVersion) {
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
        bs.plugins = [Plugins.androidApp]
        bs.android = defaultAndroidAppBlock(false)
        bs.dependencies = [
          appcompat('implementation'),
          rxlint
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private final Set<Advice> appAdvice = [
    Advice.ofChange(moduleCoordinates(rxlint), rxlint.configuration, 'runtimeOnly'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':app', appAdvice)
  ]
}
