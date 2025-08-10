// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.InlinePackageCollisionProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class InlinePackageCollisionSpec extends AbstractJvmSpec {

  def "doesn't report unused modules when there are multiple inline functions under the same package (#gradleVersion)"() {
    given:
    def project = new InlinePackageCollisionProject()
    gradleProject = project.gradleProject

    when: 'build does not fail'
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'and there is no advice'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
