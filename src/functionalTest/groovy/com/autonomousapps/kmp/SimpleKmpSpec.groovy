// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kmp

import com.autonomousapps.kmp.projects.AndroidAndJvmProject
import com.autonomousapps.kmp.projects.AndroidTargetProject
import com.autonomousapps.kmp.projects.JvmTargetProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class SimpleKmpSpec extends AbstractKmpSpec {

  def "can analyze a kmp project with jvm targets (#gradleVersion)"() {
    given:
    def project = new JvmTargetProject()
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    and:
    assertThat(result.output).contains(
      """\
        Advice for :consumer
        Unused dependencies which should be removed:
          jvmMain.dependencies {
            api("${JvmTargetProject.CAFFEINE}")
          }
        
        Existing dependencies which should be modified to be as indicated:
          commonMain.dependencies {
            api("${JvmTargetProject.OKIO}") (was commonMainImplementation)
          }""".stripIndent()
    )

    where:
    gradleVersion << gradleVersions()
  }

  def "can analyze a kmp project with android targets (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidTargetProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    and:
    assertThat(result.output).contains(
      """\
        Advice for :consumer
        Unused dependencies which should be removed:
          androidMain.dependencies {
            api("${AndroidTargetProject.CAFFEINE}")
          }
          
        These transitive dependencies should be declared directly:
          androidHostTest.dependencies {
            implementation("${AndroidTargetProject.KOTLIN_TEST}")
          }
        
        Existing dependencies which should be modified to be as indicated:
          commonMain.dependencies {
            api("${AndroidTargetProject.OKIO}") (was commonMainImplementation)
          }""".stripIndent()
    )

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }

  def "can analyze a kmp project with android and jvm targets (#gradleVersion AGP #agpVersion)"() {
    given:
    def project = new AndroidAndJvmProject(agpVersion)
    gradleProject = project.gradleProject

    when:
    def result = build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    and:
    assertThat(result.output).contains(
      """\
        Advice for :android
        Unused dependencies which should be removed:
          androidMain.dependencies {
            api("${AndroidAndJvmProject.CAFFEINE}")
          }
        
        These transitive dependencies should be declared directly:
          androidHostTest.dependencies {
            implementation("${AndroidAndJvmProject.KOTLIN_TEST}")
          }
        
        Existing dependencies which should be modified to be as indicated:
          commonMain.dependencies {
            api("${AndroidAndJvmProject.OKIO}") (was commonMainImplementation)
          }
        
        Advice for :jvm
        Unused dependencies which should be removed:
          commonMain.dependencies {
            api("${AndroidAndJvmProject.KOTLINX_COROUTINES_CORE}")
          }
        
        Existing dependencies which should be modified to be as indicated:
          commonMain.dependencies {
            api("${AndroidAndJvmProject.OKIO}") (was commonMainImplementation)
          }
          
        Advice for :multiple
        Unused dependencies which should be removed:
          androidMain.dependencies {
            api("${AndroidAndJvmProject.CAFFEINE}")
          }
          commonMain.dependencies {
            api("${AndroidAndJvmProject.KOTLINX_COROUTINES_CORE}")
          }
        
        Existing dependencies which should be modified to be as indicated:
          commonMain.dependencies {
            api("${AndroidAndJvmProject.OKIO}") (was commonMainImplementation)
          }""".stripIndent()
    )

    where:
    [gradleVersion, agpVersion] << gradleAgpMatrix()
  }
}
