package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.kotlinStdLib
import static com.autonomousapps.kit.Dependency.project

class AbiGenericsProject extends AbstractProject {

  enum SourceKind {
    METHOD, FIELD
  }

  final GradleProject gradleProject
  private final SourceKind kind

  AbiGenericsProject(SourceKind kind) {
    this.kind = kind
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = consumerSources()
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
        bs.dependencies = [
          kotlinStdLib('api'),
          project('api', ':genericsFoo'),
          project('api', ':genericsBar')
        ]
      }
    }
    builder.withSubproject('genericsFoo') { s ->
      s.sources = sourceProducerFoo
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
        bs.dependencies = [kotlinStdLib('api')]
      }
    }
    builder.withSubproject('genericsBar') { s ->
      s.sources = sourceProducerBar
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
        bs.dependencies = [kotlinStdLib('api')]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private consumerSources() {
    if (kind == SourceKind.METHOD) return sourceConsumerMethod
    else if (kind == SourceKind.FIELD) return sourceConsumerField

    throw new IllegalStateException("Unknown SourceType $kind")
  }

  def sourceConsumerMethod = [
    new Source(
      SourceType.KOTLIN, "Main", "com/example",
      """\
        package com.example
                
        class Main {
          fun useGeneric(foos: List<Foo>): Map<Foo, Bar> = TODO()
        }
      """.stripIndent()
    )
  ]

  def sourceConsumerField = [
    new Source(
      SourceType.KOTLIN, "Main", "com/example",
      """\
        package com.example
                
        class Main {
          val fooList: Map<Foo, Bar> = mapOf(Foo() to Bar())
        }
      """.stripIndent()
    )
  ]

  private sourceProducerFoo = [
    new Source(
      SourceType.KOTLIN, "Foo", "com/example",
      """\
        package com.example
        
        class Foo
      """.stripIndent()
    )
  ]

  private sourceProducerBar = [
    new Source(
      SourceType.KOTLIN, "Bar", "com/example",
      """\
        package com.example
        
        class Bar
      """.stripIndent()
    )
  ]

  List<ComprehensiveAdvice> actualBuildHealth() {
    return actualBuildHealth(gradleProject)
  }

  final List<ComprehensiveAdvice> expectedBuildHealth = emptyBuildHealthFor(
    ':proj', ':genericsFoo', ':genericsBar'
  )
}
