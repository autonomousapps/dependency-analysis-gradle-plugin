// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AbiAnnotationsProject
import com.autonomousapps.jvm.projects.AbiAnnotationsProject.Target

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AbiAnnotationsSpec extends AbstractJvmSpec {

  def "annotations on public classes are part of the abi (#gradleVersion)"() {
    given:
    def project = new AbiAnnotationsProject(Target.CLASS)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice())

    where:
    gradleVersion << gradleVersions()
  }

  def "annotations on public methods are part of the abi (#gradleVersion)"() {
    given:
    def project = new AbiAnnotationsProject(Target.METHOD)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice())

    where:
    gradleVersion << gradleVersions()
  }

  def "annotations on parameters of public methods are part of the abi (#gradleVersion)"() {
    given:
    def project = new AbiAnnotationsProject(Target.PARAMETER)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice())

    where:
    gradleVersion << gradleVersions()
  }

  def "annotation properties on public classes are part of the abi (#gradleVersion)"() {
    given:
    def project = new AbiAnnotationsProject(Target.WITH_PROPERTY)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice())

    where:
    gradleVersion << gradleVersions()
  }

  def "annotations on generic type parameters are part of the abi (#gradleVersion)"() {
    given:
    def project = new AbiAnnotationsProject(Target.TYPE_PARAMETER)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice())

    where:
    gradleVersion << gradleVersions()
  }

  def "SOURCE annotations on public classes are not part of the abi (#gradleVersion)"() {
    given:
    def project = new AbiAnnotationsProject(Target.CLASS, false)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice())

    where:
    gradleVersion << gradleVersions()
  }
}
