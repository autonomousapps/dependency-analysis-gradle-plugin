// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.FileMutationProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class FileMutationSpec extends AbstractJvmSpec {

  def "buildHealth succeeds after deleting a file (#gradleVersion)"() {
    given:
    def project = new FileMutationProject()
    gradleProject = project.gradleProject

    when: 'We build the first time'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    when: 'We delete a source file'
    project.deleteSourceFile()

    and: 'We build again'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the build health is as expected'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
