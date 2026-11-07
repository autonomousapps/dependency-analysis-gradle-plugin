// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp

import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.kmp.projects.GeneratedClassProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class GeneratedClassSpec extends AbstractKmpSpec {

  def "does not fail when a compiler plugin generates a class without a source file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new GeneratedClassProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix(AgpVersion.AGP_MAX)
  }
}
