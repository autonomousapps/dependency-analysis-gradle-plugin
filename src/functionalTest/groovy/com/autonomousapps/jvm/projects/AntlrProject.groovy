package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.AdviceHelper
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin

import static com.autonomousapps.kit.Dependency.antlr

final class AntlrProject extends AbstractProject {

  final GradleProject gradleProject

  AntlrProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { r ->
      r.withBuildScript { bs ->
        bs.plugins += [Plugin.antlrPlugin, Plugin.javaLibraryPlugin]
        bs.dependencies = [antlr()]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  List<ComprehensiveAdvice> actualBuildHealth() {
    return AdviceHelper.actualBuildHealth(gradleProject)
  }

  final List<ComprehensiveAdvice> expectedBuildHealth = [
    new ComprehensiveAdvice(":", [] as Set<Advice>, [] as Set<PluginAdvice>, false)
  ]
}
