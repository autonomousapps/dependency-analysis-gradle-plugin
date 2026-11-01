// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp

import com.autonomousapps.kmp.projects.ValueClassProject
import spock.lang.Issue

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class ValueClassSpec extends AbstractKmpSpec {

  @Issue("https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1636")
  def "a value class in the public API is an api dependency (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new ValueClassProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
