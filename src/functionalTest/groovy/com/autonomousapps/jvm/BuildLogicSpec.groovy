// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.BuildLogicProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class BuildLogicSpec extends AbstractJvmSpec {

  // This ensures that plugin dependencies preferable reference the marker artifact.
  def "gradle plugin markers are used when the plugin is used (#gradleVersion isDirect=#isDirect)"() {
    given:
    def project = new BuildLogicProject(isDirect)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    [gradleVersion, isDirect] << multivariableDataPipe(gradleVersions(), [true, false])
  }
}
