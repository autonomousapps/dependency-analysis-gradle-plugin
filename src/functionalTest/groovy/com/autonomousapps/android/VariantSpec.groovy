// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.AllVariantsProject
import com.autonomousapps.android.projects.DebugUsageProject

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

@SuppressWarnings("GroovyAssignabilityCheck")
final class VariantSpec extends AbstractAndroidSpec {

  def "plugin understands android variants (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AllVariantsProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'clean', 'buildHealth', '--no-build-cache')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "debug-test usages are reported as such (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DebugUsageProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
