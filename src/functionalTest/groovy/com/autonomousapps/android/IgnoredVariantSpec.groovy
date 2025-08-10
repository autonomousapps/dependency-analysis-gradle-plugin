// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.AllVariantsIgnoredProject
import com.autonomousapps.android.projects.ReleaseVariantIgnoredProject
import com.autonomousapps.android.projects.DebugVariantIgnoredProject

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

@SuppressWarnings("GroovyAssignabilityCheck")
final class IgnoredVariantSpec extends AbstractAndroidSpec {

  def "can ignore debug variant (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DebugVariantIgnoredProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth',)

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "can ignore release variant (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new ReleaseVariantIgnoredProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth',)

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "can ignore all (debug and release) variants (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AllVariantsIgnoredProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth',)

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
