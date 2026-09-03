// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.CoreKtxProject
import com.autonomousapps.android.projects.KtxProject
import com.autonomousapps.internal.android.AgpVersion
import spock.lang.Issue

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class KtxSpec extends AbstractAndroidSpec {

  def "ktx dependencies are treated per user configuration (#gradleVersion AGP #agpVersion, ignoreKtx=#ignoreKtx, useKtx=#useKtx)"() {
    given:
    def project = new KtxProject(agpVersion, ignoreKtx, useKtx)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    // This test is too expensive, so we're only going to test against the latest AGP
    where:
    [gradleVersion, agpVersion, ignoreKtx, useKtx] << gradleAgpMatrixPlus(AGP_LATEST_STABLE, [true, false], [true, false])
  }

  @Issue("https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1730")
  def "core-ktx is empty now (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new CoreKtxProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where: 'core-ktx:1.19.0 requires >= AGP 9.1.0'
    [gradleVersion, agpVersion] << gradleAgpMatrix(AgpVersion.AGP_MAX)
  }
}
