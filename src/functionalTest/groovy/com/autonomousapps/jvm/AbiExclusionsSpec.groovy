package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AbiExclusionsProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AbiExclusionsSpec extends AbstractJvmSpec {

  def "abi exclusion smoke test (#gradleVersion)"() {
    given:
    def project = new AbiExclusionsProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualAdvice()).containsExactlyElementsIn(project.expectedAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
