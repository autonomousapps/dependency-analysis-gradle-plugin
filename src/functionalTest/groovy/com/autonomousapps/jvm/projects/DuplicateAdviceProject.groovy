// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.okHttp

final class DuplicateAdviceProject extends AbstractProject {

  private static final OKHTTP = okHttp('implementation')

  final GradleProject gradleProject

  DuplicateAdviceProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { s ->
        s.sources = sources
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(OKHTTP)
        }
      }
      .write()
  }

  private sources = [
    Source
      .java(
        '''\
          package com.example.consumer;
  
          import okio.Buffer;
  
          public class Consumer {
            private Buffer buffer = new Buffer();
          }'''
      )
      .build(),
    Source
      .java(
        '''\
          package com.example.consumer;
  
          import okio.Buffer;
  
          public class Consumer {
            private Buffer buffer = new Buffer();
          }'''
      )
      .withSourceSet('test')
      .build(),
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> advice = [
    Advice.ofRemove(moduleCoordinates(OKHTTP), OKHTTP.configuration),
    Advice.ofAdd(moduleCoordinates('com.squareup.okio:okio:2.6.0'), 'implementation'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', advice),
  ]
}
