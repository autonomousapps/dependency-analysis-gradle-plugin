// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.AndroidKotlinInlineProject
import com.autonomousapps.android.projects.AndroidToJvmInlineProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.kit.truth.BuildResultSubject.buildResults
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

/**
 * Regression test for
 * https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/173.
 */
final class AndroidKotlinInlineSpec extends AbstractAndroidSpec {

  def "inline usage in a kotlin source set is recognized (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidKotlinInlineProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "android using JVM inline member is recognized (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidToJvmInlineProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'We detect the inline usage from Android -> JVM'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    when:
    def result = build(gradleVersion as GradleVersion, gradleProject.rootDir, ':consumer:reason', '--id', ':producer')

    then: 'And we get the expected reason'
    assertAbout(buildResults())
      .that(result).output()
      .contains('* Imports 1 inline member: com.example.producer.magic (implies implementation).')

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
