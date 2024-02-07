// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class ConstantsProject {

  final static class TopLevel extends AbstractProject {

    final GradleProject gradleProject
    private final libProject = project('api', ':lib')

    TopLevel() {
      this.gradleProject = build()
    }

    private GradleProject build() {
      return newGradleProjectBuilder()
      // consumer
        .withSubproject('proj') { s ->
          s.sources = [SOURCE_CONSUMER]
          s.withBuildScript { bs ->
            bs.plugins = kotlin
            bs.dependencies = [libProject]
          }
        }
      // producer
        .withSubproject('lib') { s ->
          s.sources = [SOURCE_PRODUCER]
          s.withBuildScript { bs ->
            bs.plugins = kotlin
          }
        }
        .write()
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
      }""".stripIndent()
    )

    private static final Source SOURCE_PRODUCER = new Source(
      SourceType.KOTLIN, 'Lib', 'com/example/library',
      """\
      package com.example.library
      
      const val CONSTANT = "magic"
      """.stripIndent()
    )

    Set<ProjectAdvice> actualBuildHealth() {
      return actualProjectAdvice(gradleProject)
    }

    private final Set<Advice> projAdvice = [
      Advice.ofChange(projectCoordinates(libProject), libProject.configuration, 'implementation')
    ]

    final Set<ProjectAdvice> expectedBuildHealth = [
      emptyProjectAdviceFor(':lib'),
      projectAdviceForDependencies(':proj', projAdvice)
    ]
  }

  final static class Nested extends AbstractProject {

    final GradleProject gradleProject

    Nested() {
      this.gradleProject = build()
    }

    private GradleProject build() {
      return newGradleProjectBuilder()
        .withSubproject('consumer') { s ->
          s.sources = consumerSources
          s.withBuildScript { bs ->
            bs.plugins = kotlin
            bs.dependencies = [project('implementation', ':producer')]
          }
        }
        .withSubproject('producer') { s ->
          s.sources = producerSources
          s.withBuildScript { bs ->
            bs.plugins = kotlin
          }
        }
        .write()
    }

    private static final List<Source> consumerSources = [new Source(
      SourceType.KOTLIN, 'Main', 'com/example/consumer',
      """\
        package com.example.consumer
        
        import com.example.producer.A.B.C
        
        class Main {        
          fun useConstant() {
            println(C)
          }
        }""".stripIndent()
    )]

    private static final List<Source> producerSources = [new Source(
      SourceType.KOTLIN, 'A', 'com/example/producer',
      """\
        package com.example.producer
        
        object A {
          object B {
            const val C = "magic"
          }
        }""".stripIndent()
    )]

    Set<ProjectAdvice> actualBuildHealth() {
      return actualProjectAdvice(gradleProject)
    }

    final Set<ProjectAdvice> expectedBuildHealth = [
      emptyProjectAdviceFor(':consumer'),
      emptyProjectAdviceFor(':producer')
    ]
  }
}
