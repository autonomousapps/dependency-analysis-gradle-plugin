// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.KtxProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings("GroovyAssignabilityCheck")
final class KtxSpec extends AbstractAndroidSpec {

  def "ktx dependencies are treated per user configuration (#gradleVersion, AGP #agpVersion, ignoreKtx=#ignoreKtx, useKtx=#useKtx)"() {
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
}
