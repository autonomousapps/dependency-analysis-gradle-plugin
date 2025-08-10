// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.NoAutoApplyProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

/** Here we test what happens when some modules don't have DAGP applied. */
final class NoAutoApplySpec extends AbstractJvmSpec {

  def "can do complete analysis with autoapply=false (#gradleVersion)"() {
    given:
    def project = new NoAutoApplyProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    gradleVersion << gradleVersions()
  }

  def "can do targeted analysis with autoapply=false (#gradleVersion)"() {
    given:
    def project = new NoAutoApplyProject(':proj1')
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    and: 'No tasks from :proj1 were requested'
    def proj1Tasks = result.tasks.findAll { it.path.startsWith(':proj1') }
    assertThat(proj1Tasks).isEmpty()

    where:
    gradleVersion << gradleVersions()
  }
}
