package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.commonsText

final class CompileOnlyProject extends AbstractAndroidProject {

  final String agpVersion
  final GradleProject gradleProject

  CompileOnlyProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { root ->
        root.gradleProperties = GradleProperties.minimalAndroidProperties()
        root.withBuildScript { bs ->
          bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
        }
      }
      .withAndroidLibProject('lib', 'com.example.lib') { lib ->
        lib.manifest = libraryManifest()
        lib.colors = null
        lib.styles = null
        lib.withBuildScript { bs ->
          bs.plugins = [Plugins.androidLib, Plugins.kotlinAndroid]
          bs.android = defaultAndroidLibBlock(true)
          bs.dependencies = [
            commonsText('compileOnly'),
          ]
        }
      }
      .write()
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':lib'),
  ]
}
