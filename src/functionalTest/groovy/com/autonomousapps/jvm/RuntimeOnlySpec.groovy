// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.ImplRuntimeTestImplConfusionProject
import com.autonomousapps.jvm.projects.TransitiveRuntimeProject
import com.autonomousapps.utils.Colors

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class RuntimeOnlySpec extends AbstractJvmSpec {

  def "don't suggest implementation to runtimeOnly when used for testImplementation (#gradleVersion)"() {
    given:
    def project = new ImplRuntimeTestImplConfusionProject()
    gradleProject = project.gradleProject

    when:
    def result = build(
      gradleVersion, gradleProject.rootDir,
      'buildHealth',
      ':lib:reason', '--id', ImplRuntimeTestImplConfusionProject.SPARK_SQL_ID,
    )

    then: 'advice is correct'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    and: 'reason makes sense and supports multiple pieces of advice for the same dependency'
    assertThat(Colors.decolorize(result.output)).contains(
      '''\
      ------------------------------------------------------------
      You asked about the dependency 'org.apache.spark:spark-sql_2.12:3.5.0'.
      You have been advised to add this dependency to 'testImplementation'.
      You have been advised to change this dependency to 'runtimeOnly' from 'implementation'.
      ------------------------------------------------------------'''.stripIndent()
    )

    where:
    gradleVersion << gradleVersions()
  }

  def "transitive dependencies with runtime capabilities are added directly (#gradleVersion)"() {
    given:
    def project = new TransitiveRuntimeProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
