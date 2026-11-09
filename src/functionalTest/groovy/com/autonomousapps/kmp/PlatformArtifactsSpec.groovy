// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp

import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.kmp.projects.PlatformArtifactsProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class PlatformArtifactsSpec extends AbstractKmpSpec {

  def "does not advise adding a platform artifact of a declared KMP dependency (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new PlatformArtifactsProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix(AgpVersion.AGP_MAX)
  }
}
