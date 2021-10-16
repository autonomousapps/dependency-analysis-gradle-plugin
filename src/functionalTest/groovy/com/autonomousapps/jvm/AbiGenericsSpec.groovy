package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AbiGenericsProject
import com.autonomousapps.jvm.projects.AbiGenericsProject.SourceKind

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AbiGenericsSpec extends AbstractJvmSpec {

  def "generic types in a method signature are part of the abi (#gradleVersion)"() {
    given:
    def project = new AbiGenericsProject(SourceKind.METHOD)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }

  def "generic types of fields are part of the abi (#gradleVersion)"() {
    given:
    def project = new AbiGenericsProject(SourceKind.FIELD)
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
