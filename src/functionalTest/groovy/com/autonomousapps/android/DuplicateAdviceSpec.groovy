// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.DuplicateAdviceProject
import spock.lang.Issue

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

@SuppressWarnings('GroovyAssignabilityCheck')
final class DuplicateAdviceSpec extends AbstractAndroidSpec {

  @Issue("https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1818")
  def "does not advise adding to all of main, test, and androidTest scopes (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new DuplicateAdviceProject(agpVersion)
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
