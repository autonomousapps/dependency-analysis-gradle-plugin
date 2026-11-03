// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.kit.gradle.Dependency.project

/**
 * A build wide enough to exercise aggregation across many projects. Every subproject declares one dependency it never
 * uses, so every subproject produces advice and the aggregate report has to carry all of it.
 */
final class ManyProjectsProject extends AbstractProject {

  final int projectCount
  final GradleProject gradleProject

  ManyProjectsProject(int projectCount = 25) {
    this.projectCount = projectCount
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()

    builder.withSubproject('unused') { s ->
      s.sources = [
        new Source(
          SourceType.JAVA, 'Unused', 'com/example/unused',
          """\
          package com.example.unused;

          public class Unused {}
          """.stripIndent()
        )
      ]
      s.withBuildScript { bs -> bs.plugins = javaLibrary }
    }

    projectCount.times { i ->
      builder.withSubproject("proj$i") { s ->
        s.sources = [
          new Source(
            SourceType.JAVA, "Main$i", 'com/example',
            """\
            package com.example;

            public class Main$i {
              public int value() {
                return $i;
              }
            }""".stripIndent()
          )
        ]
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [project('implementation', ':unused')]
        }
      }
    }

    return builder.write()
  }

  Set<String> expectedProjectPathsWithAdvice() {
    return (0..<projectCount).collect { ":proj$it".toString() }.toSet()
  }

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }
}
