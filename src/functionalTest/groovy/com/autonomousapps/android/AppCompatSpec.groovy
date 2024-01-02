// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.fixtures.AppCompatProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AppCompatSpec extends AbstractAndroidSpec {

  def "appcompat is not reported as unused when its style resources are used (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AppCompatProject(agpVersion as String)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdvice = androidProject.adviceFor(project.appSpec)
    def expectedAdvice = project.expectedAdviceForApp
    assertThat(actualAdvice).containsExactlyElementsIn(expectedAdvice)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
