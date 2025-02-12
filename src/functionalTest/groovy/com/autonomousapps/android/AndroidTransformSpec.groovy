// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android


import com.autonomousapps.android.projects.AndroidTransformProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

/** See https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1346. */
final class AndroidTransformSpec extends AbstractAndroidSpec {

  def "does not recommend replace api with implementation (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidTransformProject(agpVersion as String)
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
