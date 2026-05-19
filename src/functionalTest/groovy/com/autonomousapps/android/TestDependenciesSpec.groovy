// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.Flags
import com.autonomousapps.android.projects.TestDependenciesProject
import com.autonomousapps.android.projects.UnitTestSingleVariantProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

@SuppressWarnings('GroovyAssignabilityCheck')
final class TestDependenciesSpec extends AbstractAndroidSpec {

  def "don't advise removing test declarations when test analysis is disabled (#gradleVersion AGP #agpVersion analyzeTests=#analyzeTests)"() {
    given:
    def project = new TestDependenciesProject(agpVersion as String, analyzeTests as Boolean)
    gradleProject = project.gradleProject

    when:
    def testFlag = "-D${Flags.TEST_ANALYSIS}=$analyzeTests"
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth', testFlag)

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion, analyzeTests] << gradleAgpMatrixPlus([true, false])
  }

  // In the case where not all variants have unit tests enabled. We can just use `testImplementation`, no need for
  // `debugTestImplementation` (the latter can even be an error).
  def "don't advise adding dependency to debutTestImplementation instead of testImplementation (#gradleVersion AGP #agpVersion disableReleaseTests=#disableReleaseTests)"() {
    given:
    def project = new UnitTestSingleVariantProject(agpVersion, disableReleaseTests)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion, disableReleaseTests] << gradleAgpMatrixPlus([true, false])
  }
}
