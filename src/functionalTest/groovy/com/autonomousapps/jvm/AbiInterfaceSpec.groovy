package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.AbiInterfaceProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class AbiInterfaceSpec extends AbstractJvmSpec {

  def "can detect interfaces (#gradleVersion)"() {
    given:
    def project = new AbiInterfaceProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, 'buildHealth')

    then:
    assertThat(project.actualProjectAdvice()).containsExactlyElementsIn(project.expectedProjectAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
