package com.autonomousapps.jvm

import com.autonomousapps.jvm.projects.ConstantsProject

import static com.autonomousapps.utils.Runner.build
import static com.google.common.truth.Truth.assertThat

final class ConstantsSpec extends AbstractJvmSpec {

  def "detects top-level constants from Kotlin source (#gradleVersion)"() {
    given:
    def project = new ConstantsProject()
    gradleProject = project.gradleProject

    when:
    build(gradleVersion, gradleProject.rootDir, ':buildHealth')

    then:
    assertThat(actualAdvice('proj')).containsExactlyElementsIn(project.expectedAdvice)

    where:
    gradleVersion << gradleVersions()
  }
}
