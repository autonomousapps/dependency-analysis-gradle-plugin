package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.AdviceHelper.actualBuildHealth
import static com.autonomousapps.AdviceHelper.emptyCompAdviceFor
import static com.autonomousapps.kit.Dependency.project

final class EnumOnlyLibProject extends AbstractProject {

  final GradleProject gradleProject

  EnumOnlyLibProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    // consumer
    builder.withSubproject('proj') { s ->
      s.sources = [SOURCE_CONSUMER]
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
        bs.dependencies = [project('implementation', ':lib')]
      }
    }
    // producer
    builder.withSubproject('lib') { s ->
      s.sources = [SOURCE_PRODUCER]
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private static final Source SOURCE_CONSUMER = new Source(
    SourceType.KOTLIN, 'Main', 'com/example',
    """\
      package com.example
      
      class Main {        
        fun useConstant() {
          println(Direction.NORTH)
        }
      }
     """.stripIndent()
  )

  private static final Source SOURCE_PRODUCER = new Source(
    SourceType.KOTLIN, 'Direction', 'com/example',
    """\
      package com.example
      
      enum class Direction {
        NORTH, SOUTH, WEST, EAST
      }
     """.stripIndent()
  )

  @SuppressWarnings('GroovyAssignabilityCheck')
  List<ComprehensiveAdvice> actualBuildHealth() {
    actualBuildHealth(gradleProject)
  }

  final List<ComprehensiveAdvice> expectedBuildHealth = [
    emptyCompAdviceFor(':proj'),
    emptyCompAdviceFor(':lib'),
  ]
}
