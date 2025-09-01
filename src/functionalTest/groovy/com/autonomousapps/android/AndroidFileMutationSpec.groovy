// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.AndroidFileMutationProject
import org.gradle.util.GradleVersion

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout
import static com.google.common.truth.Truth.assertThat

final class AndroidFileMutationSpec extends AbstractAndroidSpec {

  def "buildHealth succeeds after deleting a file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidFileMutationProject(agpVersion as String)
    gradleProject = project.gradleProject

    when: 'We build the first time'
    build(
      gradleVersion as GradleVersion,
      gradleProject.rootDir,
      'buildHealth'
    )

    then: 'the build health is as expected'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedOriginalBuildHealth)

    when: 'We delete a source file'
    project.deleteSourceFile()

    and: 'We build again'
    def result = build(
      gradleVersion as GradleVersion,
      gradleProject.rootDir,
      'buildHealth'
    )

    then: 'the build health is as expected'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedDeletionBuildHealth)

    and: 'the configuration cache was used'
    assertThat(result.getOutput()).contains("Configuration cache entry reused.")

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "buildHealth succeeds after renaming a file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidFileMutationProject(agpVersion as String)
    gradleProject = project.gradleProject

    when: 'We build the first time'
    build(
      gradleVersion as GradleVersion,
      gradleProject.rootDir,
      'buildHealth'
    )

    then: 'the build health is as expected'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedOriginalBuildHealth)

    when: 'We rename and rewrite a source file'
    project.renameAndRewriteSourceFile()

    and: 'We build again'
    def result = build(
      gradleVersion as GradleVersion,
      gradleProject.rootDir,
      'buildHealth'
    )

    then: 'the build health is as expected'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedRenameBuildHealth)

    and: 'the configuration cache was used'
    assertThat(result.getOutput()).contains("Configuration cache entry reused.")

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
