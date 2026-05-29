// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.BuildLogicVersionCatalogProject
import com.autonomousapps.utils.Colors

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

// TODO(tsr): I can imagine improving this further and permitting analysis of "Gradle Jars". See ArtifactsReportTask and
//  `filterNonGradle()`.
final class BuildLogicVersionCatalogSpec extends AbstractJvmSpec {

  // The bad advice looks like this:
  //     > Task :buildHealth
  //     Advice for root project
  //     Unused dependencies which should be removed:
  //       implementation ''
  //
  // Or in model form:
  //     Advice(coordinates=FlatCoordinates(identifier=), fromConfiguration=implementation, toConfiguration=null)
  def "can handle an opaque dependency on the gradle version catalog (#gradleVersion)"() {
    given:
    def project = new BuildLogicVersionCatalogProject(true)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, '-p', 'build-logic', 'buildHealth', ':reason', '--id', 'gradle-version-catalog')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice())

    and:
    assertThat(Colors.decolorize(result.output)).contains(project.expectedReason())

    where:
    gradleVersion << gradleVersions()
  }

  def "advice contains meaningful representation of flat coordinates (#gradleVersion)"() {
    given:
    def project = new BuildLogicVersionCatalogProject(false)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, '-p', 'build-logic', 'buildHealth', ':reason', '--id', 'gradle-version-catalog')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice())

    and:
    assertThat(Colors.decolorize(result.output)).contains(project.expectedReason())

    where:
    gradleVersion << gradleVersions()
  }
}
