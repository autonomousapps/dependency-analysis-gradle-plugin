// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.VersionBumpProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class VersionBumpSpec extends AbstractJvmSpec {

  def "advice tracks a version bump with byte-identical artifacts (#gradleVersion)"() {
    given:
    def project = new VersionBumpProject()
    gradleProject = project.gradleProject

    when: 'We build the first time'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth('1.0'))

    when: 'We bump the dependency to a version with byte-identical artifacts, and build again'
    project.bumpVersion()
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the advice reflects the correct version'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth('1.1'))

    where:
    gradleVersion << gradleVersions()
  }
}
