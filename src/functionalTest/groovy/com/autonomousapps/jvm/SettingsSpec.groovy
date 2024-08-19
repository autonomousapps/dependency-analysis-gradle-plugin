package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.SettingsProject

import static com.autonomousapps.utils.Runner.build
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
}
