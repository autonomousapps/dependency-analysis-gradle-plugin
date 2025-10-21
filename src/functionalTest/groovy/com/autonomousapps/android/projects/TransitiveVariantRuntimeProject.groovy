// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.debugImplementation
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.conscryptUber

final class TransitiveVariantRuntimeProject extends AbstractAndroidProject {

  private static final conscryptUber = conscryptUber('api')
  private static final unused = debugImplementation(':unused')

  private final String agpVersion
  final GradleProject gradleProject

  TransitiveVariantRuntimeProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidLibProject('consumer', 'com.example.consumer') { s ->
        s.manifest = libraryManifest()
        s.withBuildScript { bs ->
          bs.plugins(androidLibPlugin)
          bs.android = defaultAndroidLibBlock(false, 'com.example.consumer')
          bs.dependencies(unused)
        }
      }
      .withAndroidLibProject('unused', 'com.example.unused') { s ->
        s.manifest = libraryManifest()
        s.withBuildScript { bs ->
          bs.plugins(androidLibPlugin)
          bs.android = defaultAndroidLibBlock(false, 'com.example.unused')
          bs.dependencies(conscryptUber)
        }
      }
      .write()
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofRemove(projectCoordinates(unused), unused.configuration),
    Advice.ofAdd(moduleCoordinates(conscryptUber), 'debugRuntimeOnly'),
  ]

  private final Set<Advice> unusedAdvice = [
    Advice.ofChange(moduleCoordinates(conscryptUber), conscryptUber.configuration, 'runtimeOnly'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
    projectAdviceForDependencies(':unused', unusedAdvice),
  ]
}
