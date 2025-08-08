// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.android.TestFixturesOptions
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.project

final class TestFixturesDuplicatedWithMainProject extends AbstractAndroidProject {

  final GradleProject gradleProject

  private final String agpVersion

  TestFixturesDuplicatedWithMainProject(String agpVersion) {
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
      .withSubproject('lib-test-utils') { s ->
        s.sources = libTestUtilsSources
        s.withBuildScript { bs ->
          bs.plugins = kotlin
        }
      }
      .withAndroidSubproject('lib') { s ->
        s.sources = sourcesWithTestFixtures
        s.manifest = libraryManifest('lib.with.fixtures')
        s.withBuildScript { bs ->
          bs.plugins = androidLibWithKotlin
          bs.android = defaultAndroidLibBlock(true).tap {
            testFixturesOptions = new TestFixturesOptions(true)
          }
          bs.dependencies = [
            project("implementation", ":lib-test-utils"),
            project("testFixturesImplementation", ":lib-test-utils")
          ]
        }
      }
      .withAndroidSubproject('app') { s ->
        s.sources = sourcesWithTestFixtures
        s.manifest = AndroidManifest.defaultLib('com.example.app')
        s.withBuildScript { bs ->
          bs.plugins = androidAppWithKotlin
          bs.android = defaultAndroidAppBlock(true,"com.example.app").tap {
            testFixturesOptions = new TestFixturesOptions(true)
          }
          bs.dependencies = [
            project("implementation", ":lib-test-utils"),
            project("testFixturesImplementation", ":lib-test-utils")
          ]
        }
      }
      .write()
  }

  private List<Source> libTestUtilsSources = [
    new Source(
      SourceType.KOTLIN, 'SomeUtil', 'com/example/utils',
      """\
      package com.example.utils
      
      object SomeUtils {
          fun utilFun() = Unit
      }
      """.stripIndent()
    ),
  ]

  private List<Source> sourcesWithTestFixtures = [
    new Source(
      SourceType.KOTLIN, 'ClassInImpl', 'com/example/fixtures',
      """\
      package com.example.fixtures

      import com.example.utils.SomeUtils
      
      object ClassInImpl {
          fun impl() = SomeUtils.utilFun()
      }
      """.stripIndent()
    ),
    new Source(
      SourceType.KOTLIN, 'ClassInFixtures', 'com/example/lib',
      """\
      package com.example.fixtures

      import com.example.utils.SomeUtils
      
      object ClassInFixtures {
          fun impl() = SomeUtils.utilFun()
      }
      """.stripIndent(),
      'testFixtures'
    )
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  static Set<ProjectAdvice> expectedBuildHealth() {
    [
      emptyProjectAdviceFor(':lib'),
      emptyProjectAdviceFor(':app'),
      emptyProjectAdviceFor(':lib-test-utils')
    ]
  }
}
