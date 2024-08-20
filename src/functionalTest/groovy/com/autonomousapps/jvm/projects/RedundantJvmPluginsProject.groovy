// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.kotlinStdLib

final class RedundantJvmPluginsProject extends AbstractProject {

  final GradleProject gradleProject

  RedundantJvmPluginsProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.withGroovy("""\
          dependencyAnalysis {
            issues {
              all {
                onRedundantPlugins {
                  severity 'fail'
                }
              }
            }
          }""")
        }
      }
      .withSubproject('proj') { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins = [Plugin.javaLibrary, Plugins.kotlinJvmNoVersion, Plugins.dependencyAnalysisNoVersion]
          bs.dependencies = [kotlinStdLib('api')]
        }
      }
      .write()
  }

  def sources = [
    new Source(
      SourceType.JAVA, "Main", "com/example",
      """\
        package com.example;
        
        public class Main {
          public int magic() { return 42; }
        }""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private static Set<Advice> removeKotlinStdlib = [
    Advice.ofRemove(moduleCoordinates(kotlinStdLib('api')), 'api')
  ]

  private static Set<PluginAdvice> removeKotlinJvmPlugin = [PluginAdvice.redundantKotlinJvm()]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdvice(':proj', removeKotlinStdlib, removeKotlinJvmPlugin, true)
  ]
}
