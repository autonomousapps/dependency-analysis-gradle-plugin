// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.implementation

final class NativeLibProject extends AbstractAndroidProject {

  private static final String graphicsCore = 'androidx.graphics:graphics-core:1.0.2'

  final GradleProject gradleProject
  private final String agpVersion

  NativeLibProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidLibProject('lib', 'com.example.lib') { lib ->
        lib.withBuildScript { bs ->
          bs.plugins = androidLibPlugin
          bs.android = defaultAndroidLibBlock(false, 'com.example.lib')
          bs.dependencies(implementation(graphicsCore))
        }
        lib.colors = AndroidColorRes.DEFAULT
        lib.manifest = libraryManifest('com.example.lib')
      }
      .write()
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> libAdvice = [
    Advice.ofChange(moduleCoordinates(graphicsCore), 'implementation', 'runtimeOnly')
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':lib', libAdvice),
  ]
}
