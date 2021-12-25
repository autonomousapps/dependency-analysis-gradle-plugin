package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.DefaultVariantProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class DefaultVariantSpec extends AbstractJvmSpec {

  def "static inner classes are associated with a variant (#gradleVersion)"() {
    given:
    def project = new DefaultVariantProject.Java()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).isEqualTo(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "multi-class kotlin files are associated with a variant (#gradleVersion)"() {
    given:
    def project = new DefaultVariantProject.Kotlin()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).isEqualTo(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
