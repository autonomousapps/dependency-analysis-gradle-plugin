// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.clikt

final class BundleKmpProject extends AbstractProject {

  final GradleProject gradleProject

  BundleKmpProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { c ->
        c.sources = sourcesConsumer
        c.withBuildScript { bs ->
          bs.plugins = [
            Plugins.kotlinJvmNoVersion,
            Plugin.application,
            Plugins.dependencyAnalysisNoVersion,
          ]
          bs.dependencies = [
            clikt('implementation')
          ]
        }
      }
      .write()
  }

  private sourcesConsumer = [
    new Source(
      SourceType.KOTLIN, "Consumer", "com/example/consumer",
      """\
        package com.example.consumer
        
        import com.github.ajalt.clikt.core.CliktCommand
        
        class Consumer : CliktCommand() {
          override fun run() {
          }
        }""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedProjectAdvice = [
    emptyProjectAdviceFor(':consumer')
  ]
}
