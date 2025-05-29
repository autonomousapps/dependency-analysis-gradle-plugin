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

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.AdviceHelper.moduleCoordinates
import static com.autonomousapps.AdviceHelper.projectAdviceForDependencies
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.mockitoCore

final class TestFixturesWithAbiProject extends AbstractAndroidProject {

  final GradleProject gradleProject

  private final String agpVersion

  TestFixturesWithAbiProject(String agpVersion) {
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
            mockitoCore('testFixturesImplementation'),
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

      import org.mockito.ArgumentMatcher
      import org.mockito.internal.matchers.Equals
      
      object ClassInFixtures {
          fun typePresentInApi(): ArgumentMatcher<in String> = Equals("myString")
      }
      """.stripIndent(),
      'testFixtures'
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private static Set<Advice> changeMockitoImplToApi() {
    return [
      Advice.ofChange(moduleCoordinates('org.mockito:mockito-core:4.0.0'), 'testFixturesImplementation', 'testFixturesApi'),
    ]
  }

  static Set<ProjectAdvice> expectedBuildHealth() {
    [
      projectAdviceForDependencies(':lib', changeMockitoImplToApi())
    ]
  }
}
