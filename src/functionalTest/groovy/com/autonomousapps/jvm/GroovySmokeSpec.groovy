// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.GroovySmokeProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

/** A smoke test for Groovy support. */
final class GroovySmokeSpec extends AbstractJvmSpec {

  def "groovy projects can be accurately analyzed (#gradleVersion)"() {
    given:
    def project = new GroovySmokeProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
