// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.TypeUsageProject
import com.autonomousapps.jvm.projects.TypeUsageWithFiltersProject
import com.autonomousapps.jvm.projects.TypeUsageMultiModuleProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class TypeUsageSpec extends AbstractJvmSpec {

  def "generates type usage report (#gradleVersion)"() {
    given:
    def project = new TypeUsageProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'computeTypeUsageMain')

    then:
    assertThat(project.actualTypeUsage()).isEqualTo(project.expectedTypeUsage())

    where:
    gradleVersion << gradleVersions()
  }

  def "excludes filtered types (#gradleVersion)"() {
    given:
    def project = new TypeUsageWithFiltersProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'computeTypeUsageMain')

    then:
    assertThat(project.actualTypeUsage()).isEqualTo(project.expectedTypeUsage())

    where:
    gradleVersion << gradleVersions()
  }

  def "tracks type usage across multiple modules (#gradleVersion)"() {
    given:
    def project = new TypeUsageMultiModuleProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'computeTypeUsageMain')

    then: 'app module'
    assertThat(project.actualTypeUsageFor(':app')).isEqualTo(project.expectedAppTypeUsage())

    and: 'core module'
    assertThat(project.actualTypeUsageFor(':core')).isEqualTo(project.expectedCoreTypeUsage())

    and: 'utils module'
    assertThat(project.actualTypeUsageFor(':utils')).isEqualTo(project.expectedUtilsTypeUsage())

    where:
    gradleVersion << gradleVersions()
  }
}
