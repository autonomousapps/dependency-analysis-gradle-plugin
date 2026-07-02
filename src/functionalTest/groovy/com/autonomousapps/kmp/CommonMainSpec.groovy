// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp

import com.autonomousapps.kmp.projects.CommonMain
import spock.lang.Issue

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class CommonMainSpec extends AbstractKmpSpec {

  @Issue("https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1746")
  def "does not erroneously suggest changing a dependency from commonMainApi to jvmMainApi (#gradleVersion)"() {
    given:
    def project = new CommonMain()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
