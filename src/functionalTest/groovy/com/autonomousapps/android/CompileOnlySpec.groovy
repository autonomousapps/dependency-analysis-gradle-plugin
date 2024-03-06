// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.CompileOnlyProject
import com.autonomousapps.fixtures.CompileOnlyTestProject

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class CompileOnlySpec extends AbstractAndroidSpec {

  def "compileOnly deps are never suggested to be changed (#gradleVersion AGP #agpVersion)"() {
    def project = new CompileOnlyProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdvice(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "reports dependencies that could be compileOnly (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new CompileOnlyTestProject(agpVersion)
    androidProject = project.newProject()

    when:
    build(gradleVersion, androidProject, 'buildHealth')

    then:
    def actualAdviceForApp = androidProject.adviceFor(project.appSpec)
    def expectedAdviceForApp = project.expectedAdviceForApp
    assertThat(actualAdviceForApp).containsExactlyElementsIn(expectedAdviceForApp)

    and:
    def actualAdviceForAndroidKotlinLib = androidProject.adviceFor(project.androidKotlinLib)
    def expectedAdviceForAndroidKotlinLib = project.expectedAdviceForAndroidKotlinLib
    assertThat(actualAdviceForAndroidKotlinLib).containsExactlyElementsIn(expectedAdviceForAndroidKotlinLib)

    and:
    def actualAdviceForJavaJvmLib = androidProject.adviceFor(project.javaJvmLib)
    def expectedAdviceForJavaJvmLib = project.expectedAdviceForJavaJvmLib
    assertThat(actualAdviceForJavaJvmLib).containsExactlyElementsIn(expectedAdviceForJavaJvmLib)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
