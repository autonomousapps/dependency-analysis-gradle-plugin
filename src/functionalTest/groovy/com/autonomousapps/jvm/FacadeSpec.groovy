package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.FacadeProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class FacadeSpec extends AbstractJvmSpec {

  def "suggests removing unnecessary facade (#gradleVersion)"() {
    given:
    def project = new FacadeProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
