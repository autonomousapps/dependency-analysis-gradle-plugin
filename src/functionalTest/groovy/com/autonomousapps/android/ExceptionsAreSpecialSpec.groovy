// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.ExceptionsAreSpecialProject
import com.autonomousapps.utils.Colors

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.kit.GradleBuilder.build
import static com.google.common.truth.Truth.assertAbout
import static com.google.common.truth.Truth.assertThat

class ExceptionsAreSpecialSpec extends AbstractAndroidSpec {

  @SuppressWarnings('GroovyAssignabilityCheck')
  def "exception types are correctly mapped to the providing dependencies (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new ExceptionsAreSpecialProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':buildHealth', ':consumer:reason', '--id', 'androidx.navigation:navigation-common:')

    then:
    assertAbout(buildHealth())
      .that(project.actualProjectAdvice())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedProjectAdvice())

    and:
    assertThat(Colors.decolorize(result.output)).contains(project.expectedReason())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
