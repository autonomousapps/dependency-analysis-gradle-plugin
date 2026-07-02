// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.PomRelocationProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class PomRelocationSpec extends AbstractJvmSpec {

  // nb: this now works thanks to a new feature to ensure we don't drop transitive runtimeOnly deps. It does NOT work
  // thanks to anything to do with a POM's `relocation` feature.
  def "handles POM distributionManagement.relocation (#gradleVersion)"() {
    given:
    def project = new PomRelocationProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth', ':proj:reason', '--id', 'com.mysql:mysql-connector-j')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
