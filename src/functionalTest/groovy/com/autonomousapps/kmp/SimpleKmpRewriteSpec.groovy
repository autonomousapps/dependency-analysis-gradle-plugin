// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp

import com.autonomousapps.kmp.projects.JvmTargetProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

class SimpleKmpRewriteSpec extends AbstractKmpSpec {

  def "can fix dependencies for a kmp project with jvm targets (#gradleVersion)"() {
    given:
    def project = new JvmTargetProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'fixDependencies')

    then:
    assertThat(project.actualBuildscriptContent()).isEqualTo(project.expectedBuildScriptContent())

    where:
    gradleVersion << gradleVersions()
  }
}
