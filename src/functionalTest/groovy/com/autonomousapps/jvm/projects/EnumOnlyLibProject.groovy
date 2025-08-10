// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.project

final class EnumOnlyLibProject extends AbstractProject {

  final GradleProject gradleProject

  EnumOnlyLibProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
    // consumer
      .withSubproject('proj') { s ->
        s.sources = [SOURCE_CONSUMER]
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies = [project('implementation', ':lib')]
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
      
      class Main {        
        fun useConstant() {
          println(Direction.NORTH)
        }
      }""".stripIndent()
  )

  private static final Source SOURCE_PRODUCER = new Source(
    SourceType.KOTLIN, 'Direction', 'com/example',
    """\
      package com.example
      
      enum class Direction {
        NORTH, SOUTH, WEST, EAST
      }""".stripIndent()
  )

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':proj'),
    emptyProjectAdviceFor(':lib'),
  ]
}
