package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.kit.Dependency.junit
import static com.autonomousapps.kit.Dependency.kotestAssertions
import static java.util.Collections.emptyList

final class TestBundleProject extends AbstractProject {

  private static final junit = junit('testImplementation')
  private static final kotest = kotestAssertions('testImplementation')

  final GradleProject gradleProject

  TestBundleProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
        bs.dependencies = [kotest, junit]
      }
    }
    builder.withRootProject {
      it.withBuildScript { bs ->
        bs.additions = """
          dependencyAnalysis {
            dependencies {
              bundle('kotest-assertions') {
                includeDependency('io.kotest:kotest-assertions-core-jvm')
                includeDependency('io.kotest:kotest-assertions-shared-jvm')
              }
            }
          }
        """.stripIndent()
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Source> sources = [
    new Source(
      SourceType.KOTLIN, "Main", "com/example",
      """\
        package com.example

        class Main {
          fun magic() = 42
        }
      """.stripIndent()
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
        }
      """.stripIndent(),
      'test'
    )
  ]

  final List<Advice> expectedAdvice = emptyList()
}
