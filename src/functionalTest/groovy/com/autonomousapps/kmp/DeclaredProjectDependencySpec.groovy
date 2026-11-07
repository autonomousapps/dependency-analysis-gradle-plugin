// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp

import com.autonomousapps.kmp.projects.DeclaredProjectDependencyProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class DeclaredProjectDependencySpec extends AbstractKmpSpec {

  def "does not advise adding a declared commonMain project dependency with Android and JVM targets (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DeclaredProjectDependencyProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
