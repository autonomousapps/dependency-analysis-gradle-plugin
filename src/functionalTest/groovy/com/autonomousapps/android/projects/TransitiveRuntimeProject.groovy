// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.conscryptUber

final class TransitiveRuntimeProject extends AbstractAndroidProject {

  private static final conscryptUber = conscryptUber('api')
  private static final junitJupiterEngine = api('org.junit.jupiter:junit-jupiter-engine:5.14.0')
  private static final junitPlatformEngine = testRuntimeOnly('org.junit.platform:junit-platform-engine:1.14.0')

  private final String agpVersion
  final GradleProject gradleProject

  TransitiveRuntimeProject(String agpVersion) {
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
          bs.dependencies(
            implementation(':unused'),
            testImplementation(':unused-for-test')
          )
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
      .withSubproject('unused-for-test') { s ->
        s.withBuildScript { bs ->
          bs.plugins(javaLibrary)
          bs.dependencies(junitJupiterEngine)
        }
      }
      .write()
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> consumerAdvice = [
    Advice.ofRemove(projectCoordinates(':unused'), 'implementation'),
    Advice.ofRemove(projectCoordinates(':unused-for-test'), 'testImplementation'),
    Advice.ofAdd(moduleCoordinates(conscryptUber), 'runtimeOnly'),
    Advice.ofAdd(moduleCoordinates(junitJupiterEngine), 'testRuntimeOnly'),
    Advice.ofAdd(moduleCoordinates(junitPlatformEngine), 'testRuntimeOnly'),
  ]

  private final Set<Advice> unusedAdvice = [
    Advice.ofChange(moduleCoordinates(conscryptUber), conscryptUber.configuration, 'runtimeOnly'),
  ]

  private final Set<Advice> unusedForTestAdvice = [
    Advice.ofChange(moduleCoordinates(junitJupiterEngine), junitJupiterEngine.configuration, 'runtimeOnly'),
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':consumer', consumerAdvice),
    projectAdviceForDependencies(':unused', unusedAdvice),
    projectAdviceForDependencies(':unused-for-test', unusedForTestAdvice),
  ]
}
