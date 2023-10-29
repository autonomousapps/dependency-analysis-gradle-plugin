package com.autonomousapps.android.projects

import com.autonomousapps.AdviceHelper
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.BuildscriptBlock
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Repository
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice

import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class NativeLibProject extends AbstractAndroidProject {

  final GradleProject gradleProject
  private final String agpVersion

  NativeLibProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.gradleProperties = GradleProperties.minimalAndroidProperties()
      root.withBuildScript { bs ->
        bs.buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
      }
    }
    builder.withAndroidSubproject('app') { a ->
      a.withBuildScript { bs ->
        bs.plugins = [Plugins.androidApp, Plugins.kotlinAndroid]
        bs.repositories = [Repository.LIBS]
        bs.android = androidAppBlock()
        bs.dependencies = dependencies
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Dependency> dependencies = [
    kotlinStdLib("implementation"),
    appcompat("implementation"),
    constraintLayout("implementation"),
    new Dependency("implementation", "amazon-chime-sdk-media", "aar")
  ]

  // TODO This test is currently being ignored!
  List<Advice> actualAdvice() {
    return AdviceHelper.actualAdviceForFirstSubproject(gradleProject)
  }

  final List<Advice> expectedAdvice = []
}
