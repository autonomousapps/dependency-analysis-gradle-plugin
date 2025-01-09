// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AbiGenericsProject
import com.autonomousapps.jvm.projects.AbiGenericsProject.SourceKind

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class ProjectHealthSpec extends AbstractJvmSpec {

  def "projectHealth prints build file path (#gradleVersion)"() {
    given:
    def project = new AbiGenericsProject(SourceKind.METHOD)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, 'projectHealth')

    then:
    assertThat(result.output).contains(
      """${gradleProject.rootDir.getPath()}/proj/build.gradle
        |Existing dependencies which should be modified to be as indicated:
        |  api project(':genericsBar') (was implementation)
        |  api project(':genericsFoo') (was implementation)
        |""".stripMargin())

    where:
    gradleVersion << gradleVersions()
  }
}
