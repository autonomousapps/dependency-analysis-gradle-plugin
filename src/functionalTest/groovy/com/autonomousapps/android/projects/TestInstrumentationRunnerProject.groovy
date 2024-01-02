// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android.projects

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*
import static com.autonomousapps.kit.gradle.Dependency.api
import static com.autonomousapps.kit.gradle.Dependency.project

/** https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/873. */
final class TestInstrumentationRunnerProject extends AbstractAndroidProject {

  private static final TEST_RUNNER_PACKAGE = 'com.test.testrunner'
  private static final TEST_RUNNER_CLASS = 'TestRunner'

  private final testRunner = project('androidTestImplementation', ':test_runner')

  final GradleProject gradleProject

  TestInstrumentationRunnerProject(String agpVersion) {
    super(agpVersion)
    this.gradleProject = build()
  }

  private GradleProject build() {
    return newAndroidGradleProjectBuilder(agpVersion)
      .withAndroidSubproject('app') { app ->
        app.manifest = AndroidManifest.simpleApp()
        app.withBuildScript { bs ->
          bs.plugins(Plugins.androidApp)
          bs.android = defaultAndroidAppBlock(false).tap {
            defaultConfig.testInstrumentationRunner = "$TEST_RUNNER_PACKAGE.$TEST_RUNNER_CLASS"
          }
          bs.dependencies(testRunner)
        }
      }
      .withAndroidLibProject('test_runner', 'com.test.testrunner') { lib ->
        lib.sources = sourcesTestRunner
        lib.withBuildScript { bs ->
          bs.plugins(Plugins.androidLib)
          bs.android = defaultAndroidLibBlock(false, TEST_RUNNER_PACKAGE)
          bs.dependencies(api('androidx.test:runner:1.5.2'))
        }
      }
      .write()
  }

  private sourcesTestRunner = [
    Source.java(
      """\
      package $TEST_RUNNER_PACKAGE;
      
      import androidx.test.runner.AndroidJUnitRunner;
      
      public class $TEST_RUNNER_CLASS extends AndroidJUnitRunner {}
      """
    )
      .withPath(TEST_RUNNER_PACKAGE, TEST_RUNNER_CLASS)
      .build()
  ]

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  private final Set<Advice> appAdvice = [
    Advice.ofChange(projectCoordinates(testRunner), 'androidTestImplementation', 'androidTestRuntimeOnly')
  ]

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':app', appAdvice),
    emptyProjectAdviceFor(':test_runner'),
  ]
}
