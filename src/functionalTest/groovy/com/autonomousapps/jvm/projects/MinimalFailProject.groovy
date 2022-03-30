package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.AdviceHelper.compAdviceForDependencies
import static com.autonomousapps.AdviceHelper.emptyCompAdviceFor
import static com.autonomousapps.kit.Dependency.*

final class MinimalFailProject extends AbstractProject {

  final GradleProject gradleProject

  MinimalFailProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { r ->
      r.withBuildScript { bs ->
        bs.additions = """
        dependencyAnalysis {
          strictMode(false)
          issues {
            all {
              onAny {
                severity('fail')
              }
            }
          }
        }
        """.stripMargin()
      }
    }
    builder.withSubproject('app') { s ->
      s.sources = appSources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
        bs.dependencies = [
          project('implementation', ':lib')
        ]
      }
    }
    builder.withSubproject('lib') { s ->
      s.sources = libSources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
        bs.dependencies = [
          kotlinStdlibJdk7('api'),
          moshi('api'),
          okio('api')
        ]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private static List<Source> getAppSources() {
    [
      new Source(
        SourceType.KOTLIN, "App", "com/example",
        """\
          package com.example
          
          import com.squareup.moshi.Moshi
          import okio.Buffer
          
          class App {
            // Implementation dependencies
            private val moshi = Moshi.Builder().build()
            private val buffer = Buffer()
            
            fun foo() {
              // We use Lib, so we can also use its API dependencies without declaring them
              Lib()
            }
          }
          """.stripIndent()
      )
    ]
  }

  private static List<Source> getLibSources() {
    [
      new Source(
        SourceType.KOTLIN, "Lib", "com/example",
        """\
          package com.example
          
          import com.squareup.moshi.Moshi
          import okio.Buffer
          
          class Lib {
            // Both Moshi and Buffer are part of Lib's ABI
            fun moshi(buffer: Buffer): Moshi {
              TODO("Unnecessary for TestKit test")
            }
          }
          """.stripIndent()
      )
    ]
  }

  final List<ComprehensiveAdvice> expectedAdvice = [
    compAdviceForDependencies(':app', [] as Set<Advice>),
    compAdviceForDependencies(':lib', [] as Set<Advice>)
  ]
}
