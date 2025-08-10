// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.AdviceHelper
import com.autonomousapps.android.projects.AndroidProjectWithKmpDependencies
import com.autonomousapps.android.projects.AndroidThreeTenProject
import com.autonomousapps.android.projects.FirebaseProject
import org.gradle.util.GradleVersion
import org.intellij.lang.annotations.Language

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout

final class DependenciesSpec extends AbstractAndroidSpec {

  def "threetenbp should be declared when not part of a dependency bundle (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidThreeTenProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'should add core three-ten-bp lib'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "jw threetenabp and threetenbp can be a dependency bundle (#gradleVersion AGP #agpVersion)"() {
    given:
    @Language("Groovy")
    def additions = """\
      dependencyAnalysis {
        structure {
          bundle('three-ten') {
            includeDependency('org.threeten:threetenbp')
            includeDependency('com.jakewharton.threetenabp:threetenabp')
          }
        }
      }
    """
    def project = new AndroidThreeTenProject(agpVersion as String, additions)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'no advice'
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBundleBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "firebase-analytics is a dependency bundle by default (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new FirebaseProject(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(AdviceHelper.actualProjectAdvice(gradleProject))
      .isEquivalentIgnoringModuleAdviceAndWarnings([AdviceHelper.emptyProjectAdviceFor(':app')])

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "kotlin multiplatform projects resolve canonical deps (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidProjectWithKmpDependencies(agpVersion as String)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion as GradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(AdviceHelper.actualProjectAdvice(gradleProject))
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
