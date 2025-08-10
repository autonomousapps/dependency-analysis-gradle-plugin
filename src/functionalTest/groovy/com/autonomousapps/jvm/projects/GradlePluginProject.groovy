// Copyright (c) 2025. Tony Robalik.
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

final class GradlePluginProject extends AbstractProject {

  final GradleProject gradleProject

  GradlePluginProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('plugin') { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins = [Plugin.javaGradle, Plugins.dependencyAnalysisNoVersion]
          bs.dependencies = []
        }
      }
      .write()
  }

  private List<Source> sources = [
    new Source(
      SourceType.JAVA, "MagicPlugin", "com/example",
      """\
        package com.example;
        
        import org.gradle.api.*;

        public class MagicPlugin implements Plugin<Project> {
          @Override
          public void apply(Project project) {}
        }""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':plugin'),
  ]
}
