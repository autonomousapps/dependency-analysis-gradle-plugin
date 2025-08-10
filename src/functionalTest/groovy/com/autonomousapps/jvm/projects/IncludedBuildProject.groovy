// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.testImplementation

final class IncludedBuildProject extends AbstractProject {

  private final includedBuildPath = 'second-build'
  final GradleProject gradleProject

  IncludedBuildProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { root ->
        root.withBuildScript { bs ->
          bs.plugins.add(Plugin.javaLibrary)
          bs.dependencies = [new Dependency('implementation', 'second:second-build:1.0')]
          bs.group = 'first'
          bs.version = '1.0'
        }
        root.settingsScript.additions = "includeBuild '$includedBuildPath'"
        root.sources = [
          new Source(
            SourceType.JAVA, 'Main', 'com/example/main',
            """\
            package com.example.main;
                        
            public class Main {}""".stripIndent()
          )
        ]
      }
      .withIncludedBuild(includedBuildPath) { second ->
        second.withRootProject { r ->
          r.withBuildScript { bs ->
            bs.plugins = [Plugins.dependencyAnalysis, Plugins.kotlinJvmNoApply, Plugin.javaLibrary]
            bs.dependencies = [testImplementation('first:the-project:1.0')]
            bs.group = 'second'
            bs.version = '1.0'
          }
          r.settingsScript.additions = "includeBuild('..') { name = 'the-project' }"
          r.sources = [
            new Source(
              SourceType.JAVA, 'Second', 'com/example/included',
              """\
                package com.example.included;
    
                public class Second {}""".stripIndent()
            )
          ]
        }
      }
      .write()
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  Set<ProjectAdvice> actualBuildHealthOfSecondBuild() {
    def project = gradleProject.getIncludedBuild(includedBuildPath)
    return actualProjectAdvice(project)
  }

  static Set<ProjectAdvice> expectedBuildHealth(String buildPathInAdvice) {
    [
      projectAdviceForDependencies(':', [
        Advice.ofRemove(
          includedBuildCoordinates('second:second-build',
            projectCoordinates(':', 'second:second-build', buildPathInAdvice)),
          'implementation'
        )
      ] as Set<Advice>)
    ]
  }

  static Set<ProjectAdvice> expectedBuildHealthOfIncludedBuild(String buildPathInAdvice) {
    [
      projectAdviceForDependencies(':', [
        Advice.ofRemove(
          includedBuildCoordinates('first:the-project',
            projectCoordinates(':', 'first:the-project', buildPathInAdvice)),
          'testImplementation'
        )
      ] as Set<Advice>)
    ]
  }
}
