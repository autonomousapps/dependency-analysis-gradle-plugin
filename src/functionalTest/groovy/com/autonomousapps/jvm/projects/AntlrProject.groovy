// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.AdviceHelper
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.kit.gradle.dependencies.Dependencies.antlr

final class AntlrProject extends AbstractProject {

  final GradleProject gradleProject

  AntlrProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.plugins += [Plugin.antlr, Plugin.javaLibrary, Plugins.dependencyAnalysisNoVersion]
          bs.dependencies = [antlr()]
        }
      }
      .write()
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return AdviceHelper.actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    AdviceHelper.emptyProjectAdviceFor(':')
  ]
}
