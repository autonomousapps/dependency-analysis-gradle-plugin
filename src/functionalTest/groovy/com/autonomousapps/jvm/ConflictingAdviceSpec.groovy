// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.ConflictingAdviceProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class ConflictingAdviceSpec extends AbstractJvmSpec {

  def "there is no advice to add and remove the same dependency (#gradleVersion)"() {
    given:
    def project = new ConflictingAdviceProject()
    gradleProject = project.gradleProject

    when:
    build(
      gradleVersion, gradleProject.rootDir,
      'buildHealth',
      ':lib:reason', '--id', 'com.squareup.okio:okio:3.9.1',
    )

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
