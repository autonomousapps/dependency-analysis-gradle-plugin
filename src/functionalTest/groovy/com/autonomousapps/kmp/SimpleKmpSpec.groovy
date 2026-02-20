// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp

import com.autonomousapps.kmp.projects.SimpleKmpProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class SimpleKmpSpec extends AbstractKmpSpec {

  def "can analyze a kmp project with jvm targets (#gradleVersion)"() {
    given:
    def project = new SimpleKmpProject()
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    and:
    assertThat(result.output).contains(
      '''\
        Advice for :consumer
        Unused dependencies which should be removed:
          jvmMain.dependencies {
            api("com.github.ben-manes.caffeine:caffeine:3.2.3")
          }
        
        Existing dependencies which should be modified to be as indicated:
          commonMain.dependencies {
            api("com.squareup.okio:okio:3.16.4") (was commonMainImplementation)
          }'''.stripIndent()
    )

    where:
    gradleVersion << gradleVersions()
  }
}
