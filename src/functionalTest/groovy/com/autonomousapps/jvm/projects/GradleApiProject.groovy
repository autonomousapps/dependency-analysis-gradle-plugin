package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.kit.Dependency
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin

final class GradleApiProject extends AbstractProject {

  final GradleProject gradleProject

  GradleApiProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = []
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.dependencies = [
          new Dependency('implementation', 'gradleApi()')
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  final List<Advice> expectedAdvice = []
}
