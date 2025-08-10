// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class MissingSuperclassProject extends AbstractProject {

  private final boolean checkSuperClasses
  final GradleProject gradleProject

  MissingSuperclassProject(boolean checkSuperClasses) {
    this.checkSuperClasses = checkSuperClasses
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withRootProject { r ->
        r.withBuildScript { bs ->
          // Only emit this when true to also test that the _default_ of false works
          if (checkSuperClasses) {
            bs.withGroovy(
              """\
              dependencyAnalysis {
                usage {
                  analysis {
                    checkSuperClasses true
                  }
                }
              }""".stripIndent()
            )
          }
        }
      }
      .withSubproject('a') { s ->
        s.sources = sourcesA
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary + plugins.gradleDependenciesSorter
          bs.dependencies(
            project('api', ':b'),
            // Shouldn't need this. Use it because ":b" has broken metadata
            project('implementation', ':c'),
          )
        }
      }
      .withSubproject('b') { s ->
        s.sources = sourcesB
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
          bs.dependencies(
            // Should be "api"
            project('implementation', ':c'),
          )
        }
      }
      .withSubproject('c') { s ->
        s.sources = sourcesC
        s.withBuildScript { bs ->
          bs.plugins = javaLibrary
        }
      }
      .write()
  }

  private static final List<Source> sourcesA = [
    Source.java(
      '''\
        package com.example.a;

        import com.example.b.B;

        public class A extends B {}
      '''
    )
      .withPath('com.example.a', 'A')
      .build(),
  ]

  private static final List<Source> sourcesB = [
    Source.java(
      '''\
        package com.example.b;

        import com.example.c.C;

        public class B extends C {}
      '''
    )
      .withPath('com.example.b', 'B')
      .build(),
  ]

  private static final List<Source> sourcesC = [
    Source.java(
      '''\
        package com.example.c;

        public class C {}
      '''
    )
      .withPath('com.example.c', 'C')
      .build(),
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  private Set<Advice> adviceA() {
    if (checkSuperClasses) {
      return []
    } else {
      return [
        Advice.ofRemove(projectCoordinates(':c'), 'implementation')
      ]
    }
  }

  private final Set<Advice> adviceB = [
    Advice.ofChange(projectCoordinates(':c'), 'implementation', 'api'),
  ]

  final Set<ProjectAdvice> expectedProjectAdvice() {
    return [
      projectAdviceForDependencies(':a', adviceA()),
      projectAdviceForDependencies(':b', adviceB),
      emptyProjectAdviceFor(':c'),
    ]
  }
}
