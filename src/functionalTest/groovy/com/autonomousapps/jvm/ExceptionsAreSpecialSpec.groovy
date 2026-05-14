// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.ExceptionsAreSpecialProject
import com.autonomousapps.utils.Colors

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

class ExceptionsAreSpecialSpec extends AbstractJvmSpec {

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "annotations on public classes are part of the abi (#gradleVersion isBroken=#isBroken)"() {
    given:
    def project = new ExceptionsAreSpecialProject(isBroken)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':buildHealth', ':consumer:reason', '--id', 'org.json:json')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice())

    and:
    assertThat(Colors.decolorize(result.output)).contains(project.expectedReason())

    where:
    [gradleVersion, isBroken] << multivariableDataPipe(gradleVersions(), [true, false])
  }
}
