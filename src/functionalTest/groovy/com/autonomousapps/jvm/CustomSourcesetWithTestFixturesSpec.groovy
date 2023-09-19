package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.CustomSourcesetWithTestFixturesProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class CustomSourcesetWithTestFixturesSpec extends AbstractJvmSpec {

  def "unused test dependencies are reported (#gradleVersion)"() {
    given:
    def project = new CustomSourcesetWithTestFixturesProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth())

    where:
    gradleVersion << gradleVersions()
  }

}
