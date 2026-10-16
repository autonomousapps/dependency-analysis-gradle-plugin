// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.*

final class WildcardExclusionsProject extends AbstractProject {

  final GradleProject gradleProject

    WildcardExclusionsProject() {
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
          bs.dependencies = [
            commonsCollections("implementation"),
            commonsMath("implementation"),
            dagger("implementation"),
          ]
          bs.withGroovy("""\
          dependencyAnalysis {
            issues { 
              onUnusedDependencies {
                severity('fail')
                exclude("org.apache.commons:*")
                exclude("*.dagger:*")
              }
            }
          }""")
        }
      }
      .write()
  }

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }
}
