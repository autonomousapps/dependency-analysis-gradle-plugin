// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp

import com.autonomousapps.kmp.projects.AndroidAndJvmProject
import com.autonomousapps.kmp.projects.AndroidTargetProject
import com.autonomousapps.kmp.projects.JvmTargetProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class SimpleKmpSpec extends AbstractKmpSpec {

  def "can analyze a kmp project with jvm targets (#gradleVersion)"() {
    given:
    def project = new JvmTargetProject()
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    and:
    assertThat(result.output).contains(project.expectedBuildHealthOutput())

    where:
    gradleVersion << gradleVersions()
  }

  def "can analyze a kmp project with android targets (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidTargetProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    and:
    assertThat(result.output).contains(project.expectedBuildHealthOutput())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "can analyze a kmp project with android and jvm targets (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidAndJvmProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    and:
    assertThat(result.output).contains(project.expectedBuildHealthOutput())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
