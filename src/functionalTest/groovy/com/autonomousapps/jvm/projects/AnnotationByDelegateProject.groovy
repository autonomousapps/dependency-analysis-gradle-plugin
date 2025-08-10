// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.emptyProjectAdviceFor
import static com.autonomousapps.kit.gradle.Dependency.api

final class AnnotationByDelegateProject extends AbstractProject {

  final GradleProject gradleProject

  AnnotationByDelegateProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { s ->
        s.sources = SOURCE_CONSUMER
        s.withBuildScript { bs ->
          bs.plugins = kotlin
          bs.dependencies(
            api('com.google.guava:guava:33.3.1-jre'),
          )
        }
      }
      .write()
  }

  private static final List<Source> SOURCE_CONSUMER = [
    Source.kotlin(
      '''\
        package com.example.consumer
  
        import com.google.common.util.concurrent.Service
  
        // Service has methods annotated with @CanIgnoreReturnValue, a CLASS-retained annotation. by-delegation means
        // kotlinc generates a MyService.class file which directly references that annotation.
        class MyService(private val delegate: Service) : Service by delegate
      '''
    )
      .withPath('com.example.MyService', 'MyService')
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    emptyProjectAdviceFor(':consumer'),
  ]
}

