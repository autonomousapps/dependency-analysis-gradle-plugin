package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.Dependency.project

final class ConstantsProject extends AbstractProject {

  final GradleProject gradleProject
  private final libProject = project('api', ':lib')

  ConstantsProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    // consumer
    builder.withSubproject('proj') { s ->
      s.sources = [SOURCE_CONSUMER]
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.kotlinPluginNoVersion]
        bs.dependencies = [libProject]
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
      
      import com.example.library.CONSTANT
      
      class Main {        
        fun useConstant() {
          println(CONSTANT)
        }
      }
     """.stripIndent()
  )

  private static final Source SOURCE_PRODUCER = new Source(
    SourceType.KOTLIN, 'Lib', 'com/example/library',
    """\
      package com.example.library
      
      const val CONSTANT = "magic"
     """.stripIndent()
  )

  @SuppressWarnings('GroovyAssignabilityCheck')
  List<ComprehensiveAdvice> actualBuildHealth() {
    actualBuildHealth(gradleProject)
  }

  private final projAdvice = [
    Advice.ofChange(dependency(libProject), 'implementation')
  ] as Set<Advice>

  final List<ComprehensiveAdvice> expectedBuildHealth = [
    emptyCompAdviceFor(':lib'),
    compAdviceForDependencies(':proj', projAdvice)
  ]
}
