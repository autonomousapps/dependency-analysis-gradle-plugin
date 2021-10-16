package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AntlrProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AntlrSpec extends AbstractJvmSpec {

  def "antlr dep on antlr conf is not declared unused (#gradleVersion)"() {
    given:
    def project = new AntlrProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
