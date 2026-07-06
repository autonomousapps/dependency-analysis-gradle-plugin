// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class ConcurrentModificationProject extends AbstractAndroidProject {

  final GradleProject gradleProject

  ConcurrentModificationProject(agpVersion) {
    super(agpVersion as String)
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder()
      .withAndroidLibProject('consumer') { lib ->
        lib.sources = consumerSources
        lib.withBuildScript { bs ->
          bs.plugins = androidLib(false)
          bs.android = defaultAndroidLibBlock(false)
          bs.dependencies(
            project('implementation', ':producer'),
            project('testFixturesImplementation', ':producer'),
          )
        }
      }
      .withSubproject('producer') { lib ->
        lib.sources = producerSources
        lib.withBuildScript { bs ->
          bs.plugins(javaLibrary)
        }
      }
      .write()
  }

  private consumerSources = [
    Source.java(
      """\
      package com.example.consumer;
      
      import com.example.producer.Producer;

      public class TestConsumer {
        private void function() {
          Producer producer = new Producer();
        } 
      }
      """
    )
      .withSourceSet('test')
      .build(),
  ]

  private producerSources = [
    Source.java(
      """\
      package com.example.producer;
      
      public class Producer {}
      """
    )
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofChange(projectCoordinates(':producer'), 'implementation', 'testImplementation'),
    Advice.ofChange(projectCoordinates(':producer'), 'testFixturesImplementation', 'testImplementation'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
    emptyProjectAdviceFor(':producer'),
  ]
}
