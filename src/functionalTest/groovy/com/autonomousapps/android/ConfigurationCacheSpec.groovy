// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.AndroidAssetsProject
import com.autonomousapps.internal.android.AgpVersion
import org.gradle.util.GradleVersion

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.kit.truth.BuildTaskSubject.buildTasks
import static com.autonomousapps.kit.truth.TestKitTruth.assertThat
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

final class ConfigurationCacheSpec extends AbstractAndroidSpec {

  def "buildHealth succeeds when configuration-cache flag is used (#gradleVersion AGP #agpVersion)"() {
    given: 'A complicated Android project'
    def project = new AndroidAssetsProject(agpVersion as String)
    gradleProject = project.gradleProject

    when: 'We build the first time'
    def result = build(
      gradleVersion as GradleVersion,
      gradleProject.rootDir,
      'buildHealth', '--configuration-cache'
    )

    then: 'buildHealth produces expected results'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    and: 'generateBuildHealth succeeded'
    assertAbout(buildTasks()).that(result.task(':generateBuildHealth')).succeeded()

    when: 'We build again'
    result = build(
      gradleVersion as GradleVersion,
      gradleProject.rootDir,
      'buildHealth', '--configuration-cache'
    )

    then: 'buildHealth produces expected results'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    and: 'generateBuildHealth was up-to-date'
    assertAbout(buildTasks()).that(result.task(':generateBuildHealth')).upToDate()

    and: 'This plugin is compatible with the configuration cache'
    if (AgpVersion.version(agpVersion as String) > AgpVersion.version('8.0')) {
      // AGP < 8 has a bug that prevents use of CC
      assertThat(result).output().contains('Configuration cache entry reused.')
    }

    where: 'Min support for this is Gradle 7.5'
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
