// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AbiImplementationProject
import com.autonomousapps.jvm.projects.AbiProject
import spock.lang.PendingFeature

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AbiSpec extends AbstractJvmSpec {

  def "properties on internal classes are not part of the ABI (#gradleVersion)"() {
    given:
    def project = new AbiProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice)

    where:
    gradleVersion << gradleVersions()
  }

  @PendingFeature(reason = "https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1215")
  def "class annotations are implementation when not used as annotations (#gradleVersion)"() {
    given:
    def project = new AbiImplementationProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
