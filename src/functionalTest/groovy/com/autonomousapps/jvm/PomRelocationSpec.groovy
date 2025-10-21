// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.PomRelocationProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

/**
 * See also discussion in {@link com.autonomousapps.internal.transform.StandardTransform#computeAdvice} function. There we
 * deliberately strip possible advice to declare {@code runtimeOnly} dependencies that are in the transitive graph.
 * Doing that would likely be disruptive, so a more targeted approach is to try to find some direct way of supporting
 * {@code relocation}s in a .pom file.
 */
final class PomRelocationSpec extends AbstractJvmSpec {

  // nb: this now works thanks to a new feature to ensure we don't drop transitive runtimeOnly deps. It does NOT work
  // thanks to anything to do with a POM's `relocation` feature.
  def "handles POM distributionManagement.relocation (#gradleVersion)"() {
    given:
    def project = new PomRelocationProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth', ':proj:reason', '--id', 'com.mysql:mysql-connector-j')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
