// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AntlrProject
import org.gradle.util.GradleVersion
import spock.lang.Ignore

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AntlrSpec extends AbstractJvmSpec {

  @Ignore("https://github.com/gradle/gradle/issues/25885")
  def "antlr dep on antlr conf is not declared unused (#gradleVersion)"() {
    given:
    def project = new AntlrProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
//    gradleVersion << [GradleVersion.version('8.2.1')]

  }
}
