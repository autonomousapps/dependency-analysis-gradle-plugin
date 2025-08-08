// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.ApplicationProject
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins

import static com.autonomousapps.kit.truth.artifact.BuildArtifactsSubject.buildArtifacts
import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertAbout
import static com.google.common.truth.Truth.assertThat

final class ApplicationSpec extends AbstractJvmSpec {

  def "can analyze kotlin-jvm application projects when kotlin-jvm is applied first (#gradleVersion)"() {
    given:
    def plugins = [Plugins.kotlinJvmNoVersion, Plugin.application]
    def project = new ApplicationProject(plugins, SourceType.KOTLIN)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "can analyze kotlin-jvm application projects when application is applied first (#gradleVersion)"() {
    given:
    def plugins = [Plugin.application, Plugins.kotlinJvmNoVersion]
    def project = new ApplicationProject(plugins, SourceType.KOTLIN)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth', 'proj:jar')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)
    // TODO(tsr): put this assertion somewhere else. It's only for TestKit-Truth
    assertAbout(buildArtifacts())
      .that(project.gradleProject.singleArtifact('proj', 'libs/proj.jar'))
      .jar()
      .resource('res.txt')
      .text()
      .isEqualTo('foo=bar')

    where:
    gradleVersion << gradleVersions()
  }

  def "can analyze pure java projects when forced to be application (#gradleVersion)"() {
    given:
    def plugins = [Plugin.java]
    def project = new ApplicationProject(plugins, SourceType.JAVA, true)
    gradleProject = project.gradleProject

    and:
    gradleProject.projectDir(":proj").resolve("build.gradle")

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "does not analyze pure java projects when not forced to be application (#gradleVersion)"() {
    given:
    def plugins = [Plugin.java]
    def project = new ApplicationProject(plugins)
    gradleProject = project.gradleProject

    and:
    gradleProject.projectDir(":proj").resolve("build.gradle")

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).isEmpty()

    where:
    gradleVersion << gradleVersions()
  }
}
