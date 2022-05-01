package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AbiProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AbiSpec extends AbstractJvmSpec {

  def "properties on internal classes are not part of the ABI (#gradleVersion)"() {
    given:
    def project = new AbiProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
