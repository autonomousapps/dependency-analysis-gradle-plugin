// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.DuplicateDependencyVersionsProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AllDependenciesSpec extends AbstractAndroidSpec {

  def "can generate a version catalog file with all dependencies (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DuplicateDependencyVersionsProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion as GradleVersion, gradleProject.rootDir, 'computeAllDependencies')

    then: 'all dependencies report'
    def report = project.actualAllDependencies()
    assertThat(report).isEqualTo(project.expectedAllDependencies)

    where:
    [gradleVersion, agpVersion] << multivariableDataPipe([GRADLE_8_0], [AGP_8_3.version])
  }
}
