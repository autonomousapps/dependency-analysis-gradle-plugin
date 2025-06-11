// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.android.TestFixturesOptions
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.mockitoKotlin

final class TestFixturesAddTransitiveProject extends AbstractAndroidProject {

  final GradleProject gradleProject

  private final String agpVersion

  TestFixturesAddTransitiveProject(String agpVersion) {
    super(agpVersion)
    this.agpVersion = agpVersion
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withRootProject { r ->
        r.gradleProperties = GradleProperties.minimalAndroidProperties() +
          "android.experimental.enableTestFixturesKotlinSupport=true"
      }
      .withAndroidSubproject('lib') { s ->
        s.sources = libWithFixturesSources
        s.manifest = libraryManifest('lib.with.fixtures')
        s.withBuildScript { bs ->
          bs.plugins = androidLibWithKotlin
          bs.android = defaultAndroidLibBlock(true).tap {
            testFixturesOptions = new TestFixturesOptions(true)
          }
          bs.dependencies = [
            mockitoKotlin('testFixturesImplementation'),
          ]
        }
      }
      .write()
  }

  private List<Source> libWithFixturesSources = [
    new Source(
      SourceType.KOTLIN, 'ClassInFixtures', 'com/example/lib',
      """\
      package com.example.fixtures

      import org.mockito.Mockito.verify
      import org.mockito.ArgumentMatchers.eq
      
      object ClassInFixtures {
          fun someAssert(obj: Any) = verify(obj).equals(eq("anything"))
      }
      """.stripIndent(),
      'testFixtures'
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private static Set<Advice> changeMockitoKotlinToMockitoCore() {
    return [
      Advice.ofRemove(moduleCoordinates('org.mockito.kotlin:mockito-kotlin:4.0.0'), 'testFixturesImplementation'),
      Advice.ofAdd(moduleCoordinates('org.mockito:mockito-core:4.0.0'), 'testFixturesImplementation'),
    ]
  }

  Set<ProjectAdvice> expectedBuildHealth() {
    [
      projectAdviceForDependencies(':lib', changeMockitoKotlinToMockitoCore())
    ]
  }
}
