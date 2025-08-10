// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.kit.gradle.Dependency.project

final class RegexExclusionsProject extends AbstractProject {

  final GradleProject gradleProject

  RegexExclusionsProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject(':proj') { s ->
        s.sources = [
          new Source(
            SourceType.JAVA, 'Main', 'com/example',
            """\
            package com.example;
           
            public class Main {
              public Main() {}
            
              public void hello() {
                System.out.println("hello");
              }
            }""".stripIndent()
          )
        ]
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies = [project("implementation", ":proj:internal")]
          bs.withGroovy("""\
          dependencyAnalysis {
            issues { 
              onUnusedDependencies {
                severity('fail')
                excludeRegex(".*:internal")
              }
            }
          }""")
        }
      }
      .withSubproject(':proj:internal') { s ->
        s.sources = [
          new Source(
            SourceType.JAVA, "Internal", "com/example/internal",
            """\
            package com.example.internal;
           
            public class Internal {}
            """.stripIndent()
          )
        ]
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .write()
  }

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }
}
