// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.KafkaServerProject
import com.autonomousapps.jvm.projects.MissingSuperclassProject
import com.autonomousapps.utils.Colors
import spock.lang.Ignore
import spock.lang.Issue

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class MissingSuperclassSpec extends AbstractJvmSpec {

  /**
   * If a project {@code A} has a dependency on a module {@code B} that itself has a dependency on a module {@code C},
   * where
   *
   * <pre>
   * {@code
   *   class A extends B
   *}
   * </pre>
   *
   * and
   *  <pre>
   * {@code
   * class B extends C
   *}
   * </pre>
   *
   * There are cases where module {@code B} has broken metadata -- it is not exposing module {@code C} as an {@code api}
   * dependency, which means it is missing from project {@code A}'s compile classpath. In such a case, project {@code A}
   * will need to declare an "extra" dependency on module {@code C}, even though it only needs it for compiling against
   * module {@code B} -- which means that {@code B} really ought to expose {@code C} itself.
   */
  def "advises keeping superclass dependency (#gradleVersion)"() {
    given:
    def project = new MissingSuperclassProject(true)
    gradleProject = project.gradleProject

    when:
    def result = build(
      gradleVersion, gradleProject.rootDir,
      'buildHealth',
      ':a:reason', '--id', ':c'
    )

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice())

    and:
    result.output.contains('Compiles against 1 super class or interface: com.example.c.C (implies implementation).')

    where:
    gradleVersion << [GRADLE_LATEST]
  }

  def "does not advise keeping superclass dependency when user hasn't opted-in to this analysis (#gradleVersion)"() {
    given:
    def project = new MissingSuperclassProject(false)
    gradleProject = project.gradleProject

    when:
    def result = build(
      gradleVersion, gradleProject.rootDir,
      'buildHealth',
      ':a:reason', '--id', ':c'
    )

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice())

    and:
    Colors.decolorize(result.output).contains("You have been advised to remove this dependency from 'implementation'.")

    where:
    gradleVersion << [GRADLE_LATEST]
  }

  // Leaving this has kind of extra documentation that this isn't a supported use-case (currently at least)
  @Ignore
  @Issue("https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1828")
  def "advises keeping superclass dependency for kafka-server (#gradleVersion)"() {
    given:
    def project = new KafkaServerProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth', '--rerun-tasks')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice())

    where:
    gradleVersion << gradleVersions()
  }
}
