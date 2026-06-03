// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class AnnotatedPackageProject extends AbstractProject {

  final GradleProject gradleProject

  AnnotatedPackageProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newGradleProjectBuilder()
      .withSubproject('consumer') { consumer ->
        consumer.sources = consumerSources()
        consumer.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(implementation(':annotations'))
        }
      }
      .withSubproject('annotations') { annotations ->
        annotations.sources = annotationsSources()
        annotations.withBuildScript { bs ->
          bs.plugins(javaLibrary)
        }
      }
      .write()
  }

  private static final List<Source> consumerSources() {
    [
      Source.java(
        '''\
          @Magic
          package com.consumer;
          
          import com.annotations.Magic;'''.stripIndent()
      )
        .withPath('com.consumer', 'package-info')
        .build(),
    ]
  }

  private static final List<Source> annotationsSources() {
    [
      Source.java(
        '''\
          package com.annotations;
          
          import java.lang.annotation.*;
          
          @Target(ElementType.PACKAGE)
          public @interface Magic {}'''.stripIndent()
      ).build(),
    ]
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private static final Set<Advice> consumerAdvice() {
    [Advice.ofChange(projectCoordinates(':annotations'), 'implementation', 'compileOnly')]
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice()),
    emptyProjectAdviceFor(':annotations'),
  ]
}
