package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.LombokProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class LombokSpec extends AbstractJvmSpec {

  def "can find lombok usage (#gradleVersion)"() {
    given:
    def project = new LombokProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then: 'and there is no advice'
    assertThat(project.actualBuildHealth()).containsExactlyElementsIn(project.expectedBuildHealth)

    where:
    gradleVersion << gradleVersions()
  }
}
