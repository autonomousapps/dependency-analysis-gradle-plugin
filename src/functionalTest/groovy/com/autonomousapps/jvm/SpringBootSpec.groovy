// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.SpringBootProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class SpringBootSpec extends AbstractJvmSpec {

  def "does not suggest api dependencies (#gradleVersion)"() {
    given:
    def project = new SpringBootProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where: 'Spring Boot requires Gradle 6.3+'
    gradleVersion << gradleVersions().tap {
      it.removeIf {
        it.baseVersion < GradleVersion.version('6.3').baseVersion
      }
    }
  }
}
