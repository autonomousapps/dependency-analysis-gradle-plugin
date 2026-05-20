// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation
import static com.autonomousapps.kit.gradle.Dependency.testImplementation

final class ExceptionsAreSpecialProject extends AbstractAndroidProject {

  private static final Dependency HILT_IMPL = implementation('androidx.hilt:hilt-navigation-compose:1.2.0')
  private static final Dependency LAYOUTLIB_IMPL = testImplementation('com.android.tools.layoutlib:layoutlib:15.2.3')

  final GradleProject gradleProject

  ExceptionsAreSpecialProject(String agpVersion) {
    super(agpVersion)
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidLibProject('consumer') { s ->
        s.sources = consumerSources()
        s.withBuildScript { bs ->
          bs.android = defaultAndroidLibBlock(false, 'com.example.consumer')
          bs.plugins(androidLib(false))
          bs.dependencies(
            HILT_IMPL,
            LAYOUTLIB_IMPL,
          )
        }
      }
      .write()
  }

  private static final List<Source> consumerSources() {
    return [
      Source.java(
        '''\
          package mutual.aid.consumer;

          // layoutlib bundles this class
          import android.content.Context;
          
          public abstract class ConsumerTest {
            public abstract Context getContext();
          }
        '''.stripIndent()
      )
        .withSourceSet('test')
        .build()
    ]
  }

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice() {
    [
      Advice.ofRemove(moduleCoordinates(HILT_IMPL), HILT_IMPL.configuration),

      // These two both provide runtime capabilities and would be removed with the removal of hilt, so we advise they
      // be added directly to minimize changes to the runtime classpath.
      Advice.ofAdd(moduleCoordinates('com.google.dagger:dagger-lint-aar:2.49'), 'runtimeOnly'),
      Advice.ofAdd(moduleCoordinates('org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4'), 'runtimeOnly')
    ]
  }

  Set<ProjectAdvice> expectedProjectAdvice() {
    return [projectAdviceForDependencies(':consumer', consumerAdvice())]
  }

  String expectedReason() {
    return '''\
      Source: debug, test
      -------------------
      (no usages)'''.stripIndent()
  }
}
