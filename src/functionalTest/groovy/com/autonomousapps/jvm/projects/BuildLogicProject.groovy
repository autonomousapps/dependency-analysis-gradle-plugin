// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class BuildLogicProject extends AbstractProject {

  private static final Dependency DAGP = implementation('com.autonomousapps.dependency-analysis:com.autonomousapps.dependency-analysis.gradle.plugin:3.9.0')

  final GradleProject gradleProject

  BuildLogicProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('conventions') { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins(Plugin.javaGradle, Plugins.kotlinJvmNoVersion, Plugins.dependencyAnalysisNoVersion)
          bs.dependencies(DAGP)
        }
      }
      .write()
  }

  private List<Source> sources = [
    Source.kotlin(
      '''\
        package mutual.aid
        
        import com.autonomousapps.DependencyAnalysisSubExtension
        
        class DagpConfigurer(private val dependencyAnalysis: DependencyAnalysisSubExtension)
      '''.stripIndent()
    ).build()
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':conventions'),
  ]
}
