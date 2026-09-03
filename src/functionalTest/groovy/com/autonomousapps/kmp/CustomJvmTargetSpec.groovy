// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp

import com.autonomousapps.kmp.projects.JvmDesktopProject
import spock.lang.Issue

import static com.autonomousapps.kit.GradleBuilder.build
import static com.google.common.truth.Truth.assertThat

final class CustomJvmTargetSpec extends AbstractKmpSpec {

  @Issue("https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1865")
  def "can analyze a jvm-desktop target (#gradleVersion)"() {
    given:
    def project = new JvmDesktopProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
