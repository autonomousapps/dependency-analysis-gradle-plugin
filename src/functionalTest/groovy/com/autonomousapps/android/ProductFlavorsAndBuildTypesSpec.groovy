// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.ProductFlavorsAndBuildTypesProject

import static com.autonomousapps.advice.truth.BuildHealthSubject.buildHealth
import static com.autonomousapps.kit.GradleBuilder.build
import static com.google.common.truth.Truth.assertAbout

@SuppressWarnings('GroovyAssignabilityCheck')
class ProductFlavorsAndBuildTypesSpec extends AbstractAndroidSpec {

  def "doesn't provide confusing advice regarding flavors and build types (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new ProductFlavorsAndBuildTypesProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertAbout(buildHealth())
      .that(project.actualBuildHealth())
      .isEquivalentIgnoringModuleAdviceAndWarnings(project.expectedBuildHealth)

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
