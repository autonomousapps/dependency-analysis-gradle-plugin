package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.SamInterfacesProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class SamInterfacesSpec extends AbstractJvmSpec {

  def "detects SAM interfaces (#gradleVersion)"() {
    given:
    def project = new SamInterfacesProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
