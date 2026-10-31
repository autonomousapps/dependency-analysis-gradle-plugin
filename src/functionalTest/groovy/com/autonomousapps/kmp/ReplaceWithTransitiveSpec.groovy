// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp

import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.kmp.projects.HierarchyGroupsProject
import com.autonomousapps.kmp.projects.ReplaceWithTransitiveProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

/**
 * Replacing a dep with its transitive is an interesting case for KMP projects. We remove an unused dependency and add
 * the used transitive it provides. We need to ensure it ends up on the right source set, which is tricky because we're
 * analyzing compilations, not source sets. The tests below validate that we emit the right advice in these known
 * circumstances.
 */
final class ReplaceWithTransitiveSpec extends AbstractKmpSpec {

  def "can analyze a kmp project with common sources (#gradleVersion)"() {
    given:
    def project = new ReplaceWithTransitiveProject()
    gradleProject = project.gradleProject

    when: 'buildHealth'
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then: 'buildHealth has expected results'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    when: 'fixDependencies'
    build(gradleVersion, gradleProject.rootDir, ':consumer:fixDependencies')

    then: 'fixDependencies has expected results'
    assertThat(project.actualBuildscriptContent()).isEqualTo(project.expectedBuildScriptContent())

    where:
    gradleVersion << gradleVersions()
  }

  def "can analyze a kmp project with hierarchical sources (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new HierarchyGroupsProject(agpVersion)
    gradleProject = project.gradleProject

    when: 'buildHealth'
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then: 'buildHealth has expected results'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    // TODO(tsr): this is failing to remove unused okhttp. And when adding a transitive to this same source set, it's
    //  adding it to `commonJvmMain.dependencies {}`, which will fail with `Unresolved reference 'commonJvmMain'.`.
    //getByName("commonJvmMain").dependencies {
    //  implementation("com.squareup.okhttp3:okhttp:4.6.0")
    //}
    when: 'fixDependencies'
    build(gradleVersion, gradleProject.rootDir, ':consumer:fixDependencies')

    then: 'fixDependencies has expected results'
    assertThat(project.actualBuildscriptContent()).isEqualTo(project.expectedBuildScriptContent())

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix(AgpVersion.AGP_MAX)
  }
}
