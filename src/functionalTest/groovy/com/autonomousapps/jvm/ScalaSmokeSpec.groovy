package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.ScalaSmokeProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

/** A smoke test for Scala support. */
final class ScalaSmokeSpec extends AbstractJvmSpec {

  def "scala projects can be accurately analyzed (#gradleVersion)"() {
    given:
    def project = new ScalaSmokeProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
