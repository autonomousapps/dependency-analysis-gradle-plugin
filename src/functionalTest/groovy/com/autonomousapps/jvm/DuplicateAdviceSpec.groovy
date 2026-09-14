// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.DuplicateAdviceProject
import spock.lang.Issue

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class DuplicateAdviceSpec extends AbstractJvmSpec {

  @Issue("https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1818")
  def "does not advise adding to both main and test scopes (#gradleVersion)"() {
    given:
    def project = new DuplicateAdviceProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).isEqualTo(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
