// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.ComputeDominatorTreeProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class ComputeDominatorTreeSpec extends AbstractJvmSpec {

  def "dominator tree is generated for both compile and runtime (#gradleVersion)"() {
    given:
    def project = new ComputeDominatorTreeProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'app:computeDominatorTreeMain')

    then:
    assertThat(project.actualCompileDominatorTree()).contains(project.expectedCompileDominatorTreeTotalSize())
    assertThat(project.actualRuntimeDominatorTree()).contains(project.expectedRuntimeDominatorTreeTotalSize())
    assertThat(project.actualRuntimeDominatorTreeJson()).contains(
      '{"self":":app","totalSize":64480807,"dependencies":[{"self":"com.squareup.misk:misk","size":937228,"totalSize":64480807,"dependencies"'
    )

    where:
    gradleVersion << gradleVersions()
  }
}
