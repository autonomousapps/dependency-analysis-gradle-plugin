// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.junit
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.kotestAssertions

final class TestBundleProject extends AbstractProject {

  private static final junit = junit('testImplementation')
  private static final kotest = kotestAssertions('testImplementation')

  final GradleProject gradleProject

  TestBundleProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('proj') { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins = [Plugins.kotlinNoVersion]
          bs.dependencies = [kotest, junit]
        }
      }
      .withRootProject { r ->
        r.withBuildScript { bs ->
          bs.withGroovy("""
          dependencyAnalysis {
            structure {
              // This bundle works because of special handling for -jvm and -android in 
              // BundleHandler.includeDependency()
              bundle('kotest-assertions') {
                // declared but unused 
                includeDependency('io.kotest:kotest-assertions-core')
                // undeclared but used (and provided by -core)
                includeDependency('io.kotest:kotest-assertions-shared')
              }
            }
          }""")
        }
      }.write()
  }

  private List<Source> sources = [
    new Source(
      SourceType.KOTLIN, "Main", "com/example",
      """\
        package com.example

        class Main {
          fun magic() = 42
        }""".stripIndent()
    ),
    new Source(
      SourceType.KOTLIN, "MainTest", "com/example",
      """\
        package com.example
        
        import io.kotest.matchers.shouldBe
        import org.junit.Test
        
        class MainTest {
          @Test
          public fun test() {
            "a" shouldBe "b"
          }
        }""".stripIndent(),
      'test'
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':proj'),
  ]
}
