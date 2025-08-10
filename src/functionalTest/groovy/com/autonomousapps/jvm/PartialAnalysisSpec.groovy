// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AbiAnnotationsProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class PartialAnalysisSpec extends AbstractJvmSpec {

  def "can skip analysis of some projects (#gradleVersion)"() {
    given:
    def project = new AbiAnnotationsProject(
      AbiAnnotationsProject.Target.CLASS,
      false,
      // Match any project path except those that start with `:property`
      '^((?!:property)).*$'
    )
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdviceForCustomIncludes())

    where:
    gradleVersion << gradleVersions()
  }
}
