// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.ClasspathConfusionProject
import com.autonomousapps.jvm.projects.DuplicatedDependenciesProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class TestDuplicatedDependenciesSpec extends AbstractJvmSpec {

  def "duplicated dependency declaration does not lead to wrong analysis result (#gradleVersion)"() {
    given:
    def project = new DuplicatedDependenciesProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "don't suggest removing test dependency which is also on main classpath (#gradleVersion)"() {
    given:
    def project = new ClasspathConfusionProject()
    gradleProject = project.gradleProject

    when:
    build(
      gradleVersion,
      gradleProject.rootDir,
      'buildHealth',
      ':consumer:reason', '--id', 'org.apache.commons:commons-collections4:4.4'
    )

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
