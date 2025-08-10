// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.KotlinReflectProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class KotlinReflectSpec extends AbstractJvmSpec {

  def "kotlin-reflect must be present at compile-time (#gradleVersion)"() {
    given:
    def project = new KotlinReflectProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
