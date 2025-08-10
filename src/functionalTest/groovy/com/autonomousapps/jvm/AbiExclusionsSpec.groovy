// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AbiExcludedSourceSetProject
import com.autonomousapps.jvm.projects.AbiExclusionsProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AbiExclusionsSpec extends AbstractJvmSpec {

  def "abi exclusion smoke test (#gradleVersion)"() {
    given:
    def project = new AbiExclusionsProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "can exclude custom source set from ABI analysis (#gradleVersion)"() {
    given:
    def project = new AbiExcludedSourceSetProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
