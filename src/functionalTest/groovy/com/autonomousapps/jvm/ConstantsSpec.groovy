// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.ConstantsProject
import com.autonomousapps.utils.Colors

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class ConstantsSpec extends AbstractJvmSpec {

  def "detects top-level constants from Java source (#gradleVersion)"() {
    given:
    def project = new ConstantsProject.Java()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "detects top-level constants from Java source (#gradleVersion)"() {
    given:
    def project = new ConstantsProject.Java()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "detects nested constants from Java source (#gradleVersion)"() {
    given:
    def project = new ConstantsProject.JavaNested()
    gradleProject = project.gradleProject

    when:
    def result = build(
      gradleVersion, gradleProject.rootDir,
      'buildHealth',
      ':consumer:reason', '--id', ':producer',
    )

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    and:
    assertThat(Colors.decolorize(result.output)).contains(
      '''\
        Source: main
        ------------
        * Uses 4 constants: CONSTANT, DOUBLE_CONST, FLOAT_CONST, LONG_CONST (implies implementation).'''.stripIndent()
    )

    where:
    gradleVersion << gradleVersions()
  }

  def "detects nested constants from Kotlin source (#gradleVersion)"() {
    given:
    def project = new ConstantsProject.Nested()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "detects companion object constants from Kotlin source (#gradleVersion)"() {
    given:
    def project = new ConstantsProject.CompanionObject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
