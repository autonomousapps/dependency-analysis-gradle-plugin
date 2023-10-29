package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor

final class InlinePackageCollisionProject extends AbstractProject {

  final GradleProject gradleProject

  InlinePackageCollisionProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('lib-consumer-1') { l ->
      l.withBuildScript { bs ->
        bs.plugins = [Plugins.kotlinNoVersion]
        bs.dependencies = [
          Dependency.project('implementation', ":lib-producer")
        ]
      }
      l.sources = [
        new Source(
          SourceType.KOTLIN, 'Main', 'com/example/main',
          """\
            package com.example.main
            
            import com.example.lib.foo
            
            fun main() = foo()""".stripIndent()
        )
      ]
    }
    builder.withSubproject('lib-consumer-2') { l ->
      l.withBuildScript { bs ->
        bs.plugins = [Plugins.kotlinNoVersion]
        bs.dependencies = [
          Dependency.project('implementation', ":lib-producer")
        ]
      }
      l.sources = [
        new Source(
          SourceType.KOTLIN, 'Main', 'com/example/main',
          """\
            package com.example.main
            
            import com.example.lib.bar
            
            fun main() = bar()""".stripIndent()
        )
      ]
    }
    builder.withSubproject('lib-producer') { l ->
      l.withBuildScript { bs ->
        bs.plugins = [Plugins.kotlinNoVersion]
      }
      l.sources = [
        new Source(
          SourceType.KOTLIN, 'Bar', 'com/example/lib',
          """\
            package com.example.lib
            
            inline fun bar(): Int = 1""".stripIndent()
        ),
        new Source(
          SourceType.KOTLIN, 'Foo', 'com/example/lib',
          """\
            package com.example.lib
            
            inline fun foo(): Int = 2""".stripIndent()
        )
      ]
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = emptyProjectAdviceFor(
    ':lib-consumer-1', ':lib-consumer-2', ':lib-producer'
  )
}
