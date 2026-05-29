// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp

import com.autonomousapps.kmp.projects.AndroidMainProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AndroidKmpSpec extends AbstractKmpSpec {

  def "can analyze a kmp project with android targets (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidMainProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
