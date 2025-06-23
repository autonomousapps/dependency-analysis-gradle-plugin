// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.TestFixturesAddTransitiveProject
import com.autonomousapps.android.projects.TestFixturesUnusedDependencyProject
import com.autonomousapps.android.projects.TestFixturesWithAbiProject
import com.autonomousapps.android.projects.TestFixturesDuplicatedWithMainProject
import com.autonomousapps.internal.android.AgpVersion
import org.gradle.util.GradleVersion

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

final class TestFixturesSpec extends AbstractAndroidSpec {
  
  private static final AgpVersion MINIMAL_AGP_SUPPORTING_TEST_FIXTURES = AgpVersion.version("8.5.0")

  def "should not falsely report duplicated dependencies with main source set(#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new TestFixturesDuplicatedWithMainProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix(MINIMAL_AGP_SUPPORTING_TEST_FIXTURES)
  }

  def "should advise removing unused dependency even if it is duplicated in main source set"() {
    given:
    def project = new TestFixturesUnusedDependencyProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
            .that(project.actualBuildHealth())
            .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix(MINIMAL_AGP_SUPPORTING_TEST_FIXTURES)
  }

  def "should advise to include an ABI dependency as testFixturesApi"() {
    given:
    def project = new TestFixturesWithAbiProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix(MINIMAL_AGP_SUPPORTING_TEST_FIXTURES)
  }


  def "should advise to replace an unused mockito-kotlin with a used transitive mockito-core"() {
    given:
    def project = new TestFixturesAddTransitiveProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix(MINIMAL_AGP_SUPPORTING_TEST_FIXTURES)
  }
}
