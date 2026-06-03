// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AnnotatedPackageProject
import spock.lang.Issue

import static com.autonomousapps.kit.GradleBuilder.build
import static com.google.common.truth.Truth.assertThat

final class AnnotatedPackageSpec extends AbstractJvmSpec {

  @Issue("https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1274")
  def "annotations on packages should be compileOnly (#gradleVersion)"() {
    given:
    def project = new AnnotatedPackageProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
