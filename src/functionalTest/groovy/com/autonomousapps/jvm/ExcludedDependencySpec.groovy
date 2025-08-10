// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.ExcludedDependencyProject
import com.autonomousapps.utils.Colors

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class ExcludedDependencySpec extends AbstractJvmSpec {

  def "suggests removing declared excluded dependencies (#gradleVersion)"() {
    given:
    def project = new ExcludedDependencyProject()
    gradleProject = project.gradleProject

    when:
    def result = build(
      gradleVersion, gradleProject.rootDir,
      'buildHealth',
      ':consumer:reason', '--id', 'com.squareup.okio:okio'
    )

    then: 'build health is correct'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    and: 'reason is correct'
    def output = Colors.decolorize(result.output)
    assertThat(output).contains(
      '''\
      ------------------------------------------------------------
      You asked about the dependency 'com.squareup.okio:okio'.
      You have been advised to remove this dependency from 'implementation'.
      ------------------------------------------------------------'''.stripIndent()
    )
    assertThat(output).contains(
      '''\
      Source: main
      ------------
      This dependency has been excluded from dependency resolution.
      
      Source: test
      ------------
      This dependency has been excluded from dependency resolution.'''.stripIndent()
    )

    and: 'output is correct'
    assertThat(output).contains(
      '''\
      Advice for :consumer
      Unused dependencies which should be removed:
        implementation 'com.squareup.okio:okio:2.6.0\''''.stripIndent()
    )

    where:
    gradleVersion << gradleVersions()
  }
}
