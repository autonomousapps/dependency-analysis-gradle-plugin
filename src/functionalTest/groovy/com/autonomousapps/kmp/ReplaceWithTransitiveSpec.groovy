// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp

import com.autonomousapps.kmp.projects.ReplaceWithTransitiveProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class ReplaceWithTransitiveSpec extends AbstractKmpSpec {

  def "can analyze a kmp project with jvm targets (#gradleVersion)"() {
    given:
    def project = new ReplaceWithTransitiveProject()
    gradleProject = project.gradleProject

    // TODO: cleanup
    when: 'buildHealth has expected results'
//    build(gradleVersion, gradleProject.rootDir, 'listAllCommonSourceFiles', '--rerun-tasks')
//    build(gradleVersion, gradleProject.rootDir, ':buildHealth', ':consumer:listAllCommonSourceFiles')//, '--rerun-tasks')
    build(gradleVersion, gradleProject.rootDir, ':buildHealth', '--rerun-tasks')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    // TODO
//    when: 'fixDependencies works'
//    build(gradleVersion, gradleProject.rootDir, ':consumer:fixDependencies')
//
//    then: 'fixDependencies works'

    where:
    gradleVersion << gradleVersions()
  }
}
