package com.autonomousapps.android.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.*
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.Dependency.appcompat
import static com.autonomousapps.kit.Dependency.rxlint

final class LintJarProject extends AbstractProject {

  final GradleProject gradleProject
  private final String agpVersion

  private final rxlint = rxlint('implementation')

  LintJarProject(String agpVersion) {
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
        bs.plugins = [Plugin.androidAppPlugin]
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
