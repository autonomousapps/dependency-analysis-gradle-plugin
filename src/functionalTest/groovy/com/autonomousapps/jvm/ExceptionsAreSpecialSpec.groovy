// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.ExceptionsAreSpecialProject
import com.autonomousapps.utils.Colors
import spock.lang.Ignore

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@Ignore("This feature is buggy and really complex to get right for limited utility")
class ExceptionsAreSpecialSpec extends AbstractJvmSpec {

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "exception types are required at runtime (#gradleVersion isBroken=#isBroken)"() {
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
