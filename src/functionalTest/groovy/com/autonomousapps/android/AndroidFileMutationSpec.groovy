// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.AndroidAssetMutationProject
import com.autonomousapps.android.projects.AndroidFileMutationProject
import com.autonomousapps.android.projects.AndroidResMutationProject

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings('GroovyAssignabilityCheck')
final class AndroidFileMutationSpec extends AbstractAndroidSpec {

  /*
   * Kotlin source files.
   */

  def "buildHealth succeeds after deleting a file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidFileMutationProject(agpVersion as String)
    gradleProject = project.gradleProject

    when: 'We build the first time'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the build health is as expected'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedOriginalBuildHealth)

    when: 'We delete a source file'
    project.deleteSourceFile()

    and: 'We build again'
    def result = build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the build health is as expected'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedDeletionBuildHealth)

    and: 'the configuration cache was used'
    assertThat(result.output).contains("Configuration cache entry reused.")

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "buildHealth succeeds after renaming a file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidFileMutationProject(agpVersion as String)
    gradleProject = project.gradleProject

    when: 'We build the first time'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the build health is as expected'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedOriginalBuildHealth)

    when: 'We rename and rewrite a source file'
    project.renameAndRewriteSourceFile()

    and: 'We build again'
    def result = build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the build health is as expected'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedRenameBuildHealth)

    and: 'the configuration cache was used'
    assertThat(result.output).contains("Configuration cache entry reused.")

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "buildHealth succeeds after mutating a file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidFileMutationProject(agpVersion as String)
    gradleProject = project.gradleProject

    when: 'We build the first time'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the build health is as expected'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedOriginalBuildHealth)

    when: 'We mutate a source file'
    project.mutateSourceFile()

    and: 'We build again'
    def result = build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the build health is as expected'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedMutationBuildHealth)

    and: 'the configuration cache was used'
    assertThat(result.output).contains("Configuration cache entry reused.")

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  /*
   * Android assets.
   */

  def "buildHealth succeeds after deleting an asset (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidAssetMutationProject(agpVersion as String)
    gradleProject = project.gradleProject

    when: 'We build the first time'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the build health is as expected'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedOriginalBuildHealth)

    when: 'We delete a source file'
    project.deleteAsset()

    and: 'We build again'
    def result = build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the build health is as expected'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedDeletionBuildHealth)

    and: 'the configuration cache was used'
    assertThat(result.output).contains("Configuration cache entry reused.")

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  /*
   * Android res.
   */

  def "buildHealth succeeds after deleting a res file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidResMutationProject(agpVersion as String)
    gradleProject = project.gradleProject

    when: 'We build the first time'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the build health is as expected'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedOriginalBuildHealth)

    when: 'We delete a source file'
    project.deleteRes()

    and: 'We build again'
    def result = build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the build health is as expected'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedDeletionBuildHealth)

    and: 'the configuration cache was used'
    assertThat(result.output).contains("Configuration cache entry reused.")

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  /*
   * Android layouts.
   */

  def "buildHealth succeeds after deleting a layout file (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidResMutationProject(agpVersion as String)
    gradleProject = project.gradleProject

    when: 'We build the first time'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the build health is as expected'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedOriginalBuildHealth)

    when: 'We delete a source file'
    project.deleteLayout()

    and: 'We build again'
    def result = build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the build health is as expected'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedLayoutDeletionBuildHealth)

    and: 'the configuration cache was used'
    assertThat(result.output).contains("Configuration cache entry reused.")

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
