// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class ConcurrentModificationProject extends AbstractProject {

  final GradleProject gradleProject

  ConcurrentModificationProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder(GradleProject.DslKind.KOTLIN)
      .withSubproject('consumer') { c ->
        c.sources = consumerSources
        c.withBuildScript { bs ->
          bs.plugins = kotlin + plugins.javaTestFixtures
          bs.dependencies(
            project('implementation', ':producer'),
            project('testFixturesImplementation', ':producer'),
          )
        }
      }
      .withSubproject('producer') { c ->
        c.sources = producerSources
        c.withBuildScript { bs ->
          bs.plugins = kotlin
        }
      }
      .write()
  }

  private consumerSources = [
    Source.kotlin(
      """\
      package com.example.consumer
      import com.example.producer.Producer

      fun main() = println(Producer)
      """
    )
      .withSourceSet("test")
      .withPath('com.example.consumer', 'TestConsumer')
      .build(),
  ]

  private producerSources = [
    Source.kotlin(
      """\
      package com.example.producer
      
      object Producer
      """
    )
      .withPath('com.example.producer', 'Producer')
      .build(),
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofChange(projectCoordinates(':producer'), 'implementation', 'testImplementation'),
    Advice.ofRemove(projectCoordinates(':producer'), 'testFixturesImplementation'),
  ]

  final Set<ProjectAdvice> expectedProjectAdvice = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
    emptyProjectAdviceFor(':producer'),
  ]
}
