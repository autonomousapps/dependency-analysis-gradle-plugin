// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.MixedSourceProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

/**
 * Unfortunately this spec fails to reproduce the error because I am not getting the same behavior in this environment
 * as I do in the real production environment I maintain at Square (android-register). Unclear why. For now, I'll leave
 * this test in place.
 *
 * See https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/948#issuecomment-1711177139
 */
final class MixedSourceSpec extends AbstractAndroidSpec {

  def "can analyze mixed java-kotlin source sets (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new MixedSourceProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .containsExactlyDependencyAdviceIn(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
