package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.SettingsProject

import static com.autonomousapps.utils.Runner.build
import static com.autonomousapps.utils.Runner.buildAndFail
import static com.google.common.truth.Truth.assertThat

final class SettingsSpec extends AbstractJvmSpec {

  def "BuildHealthPlugin can be applied to settings script in jvm project (#gradleVersion)"() {
    given:
    def project = new SettingsProject.ScalaProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersionsSettingsApi()
  }

  def "BuildHealthPlugin can be applied to settings script in kotlin project (#gradleVersion)"() {
    given:
    def project = new SettingsProject.KotlinProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersionsSettingsApi()
  }

  def "configuration fails with useful error when KGP not on classpath (#gradleVersion)"() {
    given:
    def project = new SettingsProject.KgpMissingProject()
    gradleProject = project.gradleProject

    when:
    def result = buildAndFail(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    result.output.contains('Kotlin Gradle Plugin (KGP) not found on classpath')

    where:
    gradleVersion << gradleVersionsSettingsApi()
  }
}
