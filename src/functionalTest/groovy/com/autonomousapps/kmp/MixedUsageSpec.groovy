// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp

import com.autonomousapps.kmp.projects.MixedUsageProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class MixedUsageSpec extends AbstractKmpSpec {

  def "does not move a dependency from commonMain to androidMain when androidMain exposes it (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new MixedUsageProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
