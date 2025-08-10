// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.JavaModulesProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

// https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/800
final class JavaModulesSpec extends AbstractJvmSpec {

  def "respects package visibility of Java Modules - no advice (#gradleVersion)"() {
    given:
    def project = new JavaModulesProject(false)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealthImplementation)

    where:
    gradleVersion << gradleVersions()
  }

  def "respects package visibility of Java Modules - move advice (#gradleVersion)"() {
    given:
    def project = new JavaModulesProject(true)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealthApi)

    where:
    gradleVersion << gradleVersions()
  }
}
