// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.api
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class BuildLogicProject extends AbstractProject {

  private static final Dependency DAGP = api('com.autonomousapps.dependency-analysis:com.autonomousapps.dependency-analysis.gradle.plugin:3.9.0')
  private static final String PROVIDES_DAGP = ':provides-dagp'

  private final boolean isDirect
  final GradleProject gradleProject

  BuildLogicProject(boolean isDirect) {
    this.isDirect = isDirect
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('conventions') { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins(Plugin.javaGradle, Plugins.kotlinJvmNoVersion, Plugins.dependencyAnalysisNoVersion)
          bs.dependencies(dependencies())
        }
      }
      .withSubproject(PROVIDES_DAGP) { s ->
        s.withBuildScript { bs ->
          bs.plugins(Plugin.javaLibrary)
          bs.dependencies(DAGP)
        }
      }
      .write()
  }

  private Dependency dependencies() {
    if (isDirect) {
      DAGP
    } else {
      implementation(PROVIDES_DAGP)
    }
  }

  private List<Source> sources = [
    Source.kotlin(
      '''\
        package mutual.aid
        
        import com.autonomousapps.DependencyAnalysisSubExtension
        
        class DagpConfigurer(val dependencyAnalysis: DependencyAnalysisSubExtension)
      '''.stripIndent()
    ).build()
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private Set<Advice> conventionsAdvice() {
    if (isDirect) {
      []
    } else {
      [
        Advice.ofAdd(moduleCoordinates(DAGP), 'api'),
        Advice.ofRemove(projectCoordinates(PROVIDES_DAGP), 'implementation')
      ]
    }
  }

  final Set<ProjectAdvice> expectedBuildHealth() {
    [
      projectAdviceForDependencies(':conventions', conventionsAdvice()),
    ]
  }
}
