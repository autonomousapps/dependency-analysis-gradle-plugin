// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.GuavaProject

import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertThat

final class GuavaCheckSpec extends AbstractJvmSpec {

  def "fails if guava is below minimum version (#gradleVersion)"() {
    given:
    def project = new GuavaProject()
    gradleProject = project.gradleProject

    when: 'We run a task that requires Guava 33.1.0+'
    def result = buildAndFail(gradleVersion, gradleProject.rootDir, ':lib:computeDominatorTreeCompileMain')

    then:
    assertThat(result.output).contains('The Dependency Analysis Gradle Plugin requires Guava 33.1.0 or higher.')

    where:
    gradleVersion << gradleVersions()
  }
}
