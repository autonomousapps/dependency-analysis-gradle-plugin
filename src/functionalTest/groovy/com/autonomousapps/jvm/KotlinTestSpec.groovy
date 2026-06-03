// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.KotlinTestProject

import static com.autonomousapps.kit.GradleBuilder.build
import static com.google.common.truth.Truth.assertThat

final class KotlinTestSpec extends AbstractJvmSpec {

  def "kotlin-test is not unused when the kotlin.test.Test annotation is used (#gradleVersion)"() {
    given:
    def project = new KotlinTestProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
