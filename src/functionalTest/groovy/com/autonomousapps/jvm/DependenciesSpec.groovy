// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.KotlinStdlibProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class DependenciesSpec extends AbstractJvmSpec {

  def "kotlin stdlib is a dependency bundle by default (#gradleVersion)"() {
    given:
    def project = new KotlinStdlibProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'advice to change stdlib deps'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBundleBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
