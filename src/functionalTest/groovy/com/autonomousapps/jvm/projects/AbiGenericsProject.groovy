package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.kotlinStdLib
import static com.autonomousapps.kit.gradle.Dependency.project

final class AbiGenericsProject extends AbstractProject {

  enum SourceKind {
    CLASS, METHOD, FIELD
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
        bs.plugins = [Plugin.kotlinNoVersion]
        bs.dependencies = [
          kotlinStdLib('api'),
          project('implementation', ':genericsFoo'),
          project('implementation', ':genericsBar')
        ]
      }
    }
    builder.withSubproject('genericsFoo') { s ->
      s.sources = sourceProducerFoo
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinNoVersion]
        bs.dependencies = [kotlinStdLib('api')]
      }
    }
    builder.withSubproject('genericsBar') { s ->
      s.sources = sourceProducerBar
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinNoVersion]
        bs.dependencies = [kotlinStdLib('api')]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private consumerSources() {
    if (kind == SourceKind.CLASS) {
      return sourceConsumerClass
    } else if (kind == SourceKind.METHOD) {
      return sourceConsumerMethod
    } else if (kind == SourceKind.FIELD) return sourceConsumerField

    throw new IllegalStateException("Unknown SourceType $kind")
  }

  def sourceConsumerClass = [
    new Source(
      SourceType.KOTLIN, "Child", "com/example",
      """\
        package com.example
        
        import com.example.foo.Foo
        import com.example.bar.Bar
        
        interface Child<T : Bar> : Parent<Unit, Foo>
      """.stripIndent()
    ),
    new Source(
      SourceType.KOTLIN, "Parent", "com/example",
      """\
        package com.example
        
        interface Parent<T, U>
      """.stripIndent()
    )
  ]

  def sourceConsumerMethod = [
    new Source(
      SourceType.KOTLIN, "Main", "com/example",
      """\
        package com.example
        
        import com.example.foo.Foo
        import com.example.bar.Bar
        
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

        import com.example.foo.Foo
        import com.example.bar.Bar
        
        class Main {
          val fooList: Map<Foo, Bar> = mapOf(Foo() to Bar())
        }
      """.stripIndent()
    )
  ]

  private sourceProducerFoo = [
    new Source(
      SourceType.KOTLIN, "Foo", "com/example/foo",
      """\
        package com.example.foo
        
        class Foo
      """.stripIndent()
    )
  ]

  private sourceProducerBar = [
    new Source(
      SourceType.KOTLIN, "Bar", "com/example/bar",
      """\
        package com.example.bar
        
        class Bar
      """.stripIndent()
    )
  ]

  private final Set<Advice> expectedAdvice = [
    Advice.ofChange(projectCoordinates(':genericsFoo'), 'implementation', 'api'),
    Advice.ofChange(projectCoordinates(':genericsBar'), 'implementation', 'api')
  ]

  private final projAdvice = projectAdviceForDependencies(
    ':proj', expectedAdvice
  )

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedProjectAdvice = [
    projAdvice, emptyProjectAdviceFor(':genericsFoo'), emptyProjectAdviceFor(':genericsBar')
  ]
}
