// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.android

import com.autonomousapps.android.projects.PublicTypeUsageProject
import com.autonomousapps.internal.android.AgpVersion

import static com.autonomousapps.kit.GradleBuilder.build
import static com.google.common.truth.Truth.assertThat

@SuppressWarnings('GroovyAssignabilityCheck')
class PublicTypeUsageSpec extends AbstractAndroidSpec {

  def "type usage can be configured and computed globally (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new PublicTypeUsageProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':publicTypeUsage')

    then:
    assertThat(project.actualPublicTypeUsage()).isEqualTo(project.expectedPublicTypeUsage)

    and:
    assertThat(result.output).contains(
      '''\
        These projects have public types (classes, interfaces) that have no external accessors. Those types' visibilities could be restricted (e.g., made `internal`).
        
        :java-lib (2 types)
        - com.example.javalib.AnotherPublicUnusedClass
        - com.example.javalib.PublicUnusedClass

        :module-fire (1 type)
        - com.example.module.RealMagic
        
        :module-water (1 type)
        - com.example.module.RealMagic'''.stripIndent()
    )

    where: 'Requires at least AGP 9.1.0 due to Dagger incompatibility with earlier versions'
    [gradleVersion, agpVersion] << gradleAgpMatrix(AgpVersion.AGP_MAX)
  }
}
