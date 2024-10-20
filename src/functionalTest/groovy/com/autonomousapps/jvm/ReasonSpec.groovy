// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.AbstractFunctionalSpec
import com.autonomousapps.jvm.projects.BundleKmpProject2
import com.autonomousapps.jvm.projects.NestedSubprojectsProject
import com.autonomousapps.utils.Colors
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import spock.lang.PendingFeature

import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertThat

final class ReasonSpec extends AbstractFunctionalSpec {

  def "can discover reason for project dependency defined by project path (#gradleVersion)"() {
    given:
    gradleProject = new NestedSubprojectsProject().gradleProject
    def id = ':featureA:public'

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':featureC:public:reason', '--id', id)

    then:
    outputMatchesForProject(result, id)

    where:
    gradleVersion << gradleVersions()
  }

  @PendingFeature(reason = "ReasonTask doesn't support this because ProjectAdvice.dependencyAdvice uses ProjectCoordinates, not IncludedBuildCoordinates")
  def "can discover reason for project dependency defined by coordinates (#gradleVersion)"() {
    given:
    gradleProject = new NestedSubprojectsProject().gradleProject
    def id = 'the-project.featureA:public'

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':featureC:public:reason', '--id', id)

    then:
    outputMatchesForProject(result, id)

    where:
    gradleVersion << gradleVersions()
  }

  def "reason fails when there is dependency filtering ambiguity (#gradleVersion)"() {
    given:
    def project = new BundleKmpProject2()
    gradleProject = project.gradleProject

    when:
    def result = buildAndFail(gradleVersion, gradleProject.rootDir, ':consumer:reason', '--id', 'com.squareup.okio:oki')

    then:
    assertThat(result.output).contains("> Coordinates 'com.squareup.okio:oki' matches more than 1 dependency [com.squareup.okio:okio-jvm:3.0.0, com.squareup.okio:okio:3.0.0]")

    where:
    gradleVersion << [GradleVersion.current()]
  }

  def "reason matches startsWith when there is no ambiguity (#gradleVersion)"() {
    given:
    def project = new BundleKmpProject2()
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':consumer:reason', '--id', 'com.squareup.okio:okio-')

    then:
    assertThat(result.output).contains("You asked about the dependency 'com.squareup.okio:okio-jvm:3.0.0'.")

    where:
    gradleVersion << [GradleVersion.current()]
  }

  private static void outputMatchesForProject(BuildResult result, String id) {
    def lines = Colors.decolorize(result.output).readLines()
    def asked = lines.find { it.startsWith('You asked about') }
    def advised = lines.find { it.startsWith('You have been advised') }

    assertThat(asked).isEqualTo("You asked about the dependency '$id'.".toString())
    assertThat(advised).isEqualTo("You have been advised to remove this dependency from 'api'.")
  }
}
