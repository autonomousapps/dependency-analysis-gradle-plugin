// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor

final class KotlinStdlibProject extends AbstractProject {

  final GradleProject gradleProject
  private final String additions

  KotlinStdlibProject(String additions = '') {
    this.additions = additions
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { root ->
        root.withBuildScript { buildScript ->
          buildScript.additions = additions
        }
      }
      .withSubproject('proj') { subproject ->
        subproject.sources = sources
        subproject.withBuildScript { buildScript ->
          buildScript.plugins = kotlin
        }
      }
      .write()
  }

  private sources = [
    new Source(
      SourceType.KOTLIN, 'Library', 'com/example',
      """\
        package com.example
      
        class Library {
          fun magic() = 42
        }""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBundleBuildHealth = [
    emptyProjectAdviceFor(':proj'),
  ]
}
