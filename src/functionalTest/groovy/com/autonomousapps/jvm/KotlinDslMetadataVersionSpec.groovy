// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.KotlinDslMetadataVersionProject
import spock.lang.Issue

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class KotlinDslMetadataVersionSpec extends AbstractJvmSpec {

  @Issue('https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1671')
  def "applies on Gradle 8.x with Kotlin DSL and the metadata version check enabled (#gradleVersion)"() {
    given:
    def project = new KotlinDslMetadataVersionProject()
    gradleProject = project.gradleProject

    when: 'the Kotlin DSL scripts compile against the plugin classpath with the metadata check enabled, and buildHealth runs'
    // Before the fix, this failed during script compilation on Gradle 8.x with:
    //   "Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 2.2.0,
    //    expected version is 2.0.0."
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'the build succeeds and there is no advice'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
