// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor

final class KotlinTestProject extends AbstractProject {

  final GradleProject gradleProject

  KotlinTestProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { consumer ->
        consumer.sources = consumerSources()
        consumer.withBuildScript { bs ->
          bs.plugins(kotlin)
          bs.dependencies(
            dependencies.kotlinTest('testImplementation'),
          )
          bs.withGroovy(
            '''\
              tasks.test { useJUnitPlatform() }
            '''.stripIndent()
          )
        }
      }
      .write()
  }

  private static final List<Source> consumerSources() {
    [
      Source.kotlin(
        '''\
          package com.consumer
          
          fun main() {
            println("Hello, world!")
          }'''.stripIndent()
      )
        .withPath('com.consumer', 'main')
        .build(),
      Source.kotlin(
        '''\
          package com.consumer

          import kotlin.test.Test
          import org.junit.jupiter.api.assertDoesNotThrow
          
          class HelloWorldTest {
            @Test
            fun `runs without throwing`() {
              assertDoesNotThrow { main() }
            }
          }
        '''.stripIndent()
      )
        .withSourceSet('test')
        .build()
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
  ]
}
