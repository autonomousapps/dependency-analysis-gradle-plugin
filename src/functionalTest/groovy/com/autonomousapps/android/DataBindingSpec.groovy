// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.DataBindingProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class DataBindingSpec extends AbstractAndroidSpec {

  def "dataBinding dependencies are not reported (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DataBindingProject(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    assertThat(androidProject.adviceFor(project.appSpec)).containsExactlyElementsIn(project.expectedAdviceForApp)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
