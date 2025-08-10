// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.okio3

final class BundleKmpProject2 extends AbstractProject {

  private final okio3 = okio3('api')
  final GradleProject gradleProject

  BundleKmpProject2() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { s ->
        s.sources = sourcesConsumer
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies = [
            // gets okio-jvm from this
            project('api', ':producer')
          ]
        }
      }
      .withSubproject('producer') { s ->
        s.sources = sourcesProducer
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies = [okio3]
        }
      }
      .write()
  }

  private sourcesConsumer = [
    Source.kotlin(
      '''
        package com.example.consumer
        
        import okio.ByteString
        
        interface Consumer {
          fun string(): ByteString
        }
        '''
    )
      .withPath('com.example.consumer', 'Consumer')
      .build(),
    Source.kotlin(
      '''
        package com.example.consumer
        
        import com.example.producer.Producer
        
        abstract class ABC : Producer
        '''
    )
      .withPath('com.example.consumer', 'ABC')
      .build()
  ]

  private sourcesProducer = [
    Source.kotlin(
      '''
        package com.example.producer
        
        import okio.ByteString
        
        interface Producer {
          fun string(): ByteString
        }
      '''
    )
      .withPath('com.example.producer', 'Producer')
      .build()
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofAdd(moduleCoordinates(okio3), 'api')
  ]

  final Set<ProjectAdvice> expectedProjectAdvice = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
    emptyProjectAdviceFor(':producer')
  ]
}
