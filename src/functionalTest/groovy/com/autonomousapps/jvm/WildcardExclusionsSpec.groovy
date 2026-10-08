// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm


import com.autonomousapps.jvm.projects.WildcardExclusionsProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

class WildcardExclusionsSpec extends AbstractJvmSpec {
  def "project can exclude dependencies by wildcard (#gradleVersion)"() {
    given:
    def project = new WildcardExclusionsProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, "buildHealth")

    then:
    assertThat(project.actualProjectAdvice().isEmpty())

    where:
    gradleVersion << gradleVersions()
  }
}
